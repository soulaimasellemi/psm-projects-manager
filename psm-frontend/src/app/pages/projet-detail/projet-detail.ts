import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  CdkDrag, CdkDragDrop, CdkDragPlaceholder, CdkDropList, CdkDropListGroup, moveItemInArray, transferArrayItem,
} from '@angular/cdk/drag-drop';
import { forkJoin } from 'rxjs';
import { ProjetService, TacheService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import {
  COLONNES_KANBAN, NIVEAUX_SANTE, PRIORITES, PrioriteTache, Projet, StatutTache, Tache, initiales, statutInfo,
} from '../../core/models';

type Colonnes = Record<StatutTache, Tache[]>;

/**
 * Page détail d'un projet : informations, santé (score de risque) et tableau Kanban des tâches.
 * Glisser-déposer : ADMIN ou membre de l'équipe du projet.
 */
@Component({
  selector: 'app-projet-detail',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, RouterLink, CdkDropListGroup, CdkDropList, CdkDrag, CdkDragPlaceholder],
  templateUrl: './projet-detail.html',
})
export class ProjetDetail implements OnInit {
  /** Paramètre de route :id (withComponentInputBinding) */
  readonly id = input.required<string>();

  private projetService = inject(ProjetService);
  private tacheService = inject(TacheService);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);
  auth = inject(AuthService);

  readonly colonnesKanban = COLONNES_KANBAN;
  readonly priorites = PRIORITES;
  readonly listePriorites = Object.keys(PRIORITES) as PrioriteTache[];
  readonly niveaux = NIVEAUX_SANTE;
  readonly statutInfo = statutInfo;
  readonly initiales = initiales;

  readonly projet = signal<Projet | null>(null);
  readonly colonnes = signal<Colonnes>({ A_FAIRE: [], EN_COURS: [], TERMINEE: [] });
  readonly loading = signal(true);

  // Fenêtre tâche (création / modification)
  readonly modalOpen = signal(false);
  readonly editing = signal<Tache | null>(null);
  readonly saving = signal(false);
  readonly submitted = signal(false);

  form = this.fb.nonNullable.group({
    titre: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', Validators.maxLength(1000)],
    statut: this.fb.nonNullable.control<StatutTache>('A_FAIRE'),
    priorite: this.fb.nonNullable.control<PrioriteTache>('MOYENNE'),
    echeance: [''],
    assigneId: this.fb.control<number | null>(null),
  });

  /** ADMIN ou membre de l'équipe : peut ajouter, modifier et déplacer les tâches */
  readonly peutGerer = computed(() => {
    const p = this.projet();
    const me = this.auth.user();
    if (!p || !me) return false;
    return this.auth.isAdmin() || p.membres.some((m) => m.id === me.id);
  });

  ngOnInit() {
    this.charger();
  }

  charger() {
    const id = Number(this.id());
    forkJoin({ projet: this.projetService.get(id), taches: this.tacheService.list(id) }).subscribe({
      next: ({ projet, taches }) => {
        this.projet.set(projet);
        this.repartir(taches);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.apiError(e, 'Projet introuvable.');
        this.router.navigate(['/projets']);
      },
    });
  }

  /** Recharge le projet seul (avancement et santé recalculés par le serveur) */
  private rafraichirProjet() {
    this.projetService.get(Number(this.id())).subscribe({ next: (p) => this.projet.set(p) });
  }

  private repartir(taches: Tache[]) {
    const c: Colonnes = { A_FAIRE: [], EN_COURS: [], TERMINEE: [] };
    [...taches].sort((a, b) => a.ordre - b.ordre).forEach((t) => c[t.statut].push(t));
    this.colonnes.set(c);
  }

  // ---------- Kanban : glisser-déposer ----------

  drop(event: CdkDragDrop<Tache[]>, statut: StatutTache) {
    if (!this.peutGerer()) return;
    const tache = event.item.data as Tache;
    if (event.previousContainer === event.container && event.previousIndex === event.currentIndex) return;

    // Mise à jour immédiate de l'écran (optimiste), puis enregistrement côté serveur
    const c = this.copie();
    const source = c[tache.statut];
    if (event.previousContainer === event.container) {
      moveItemInArray(c[statut], event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(source, c[statut], event.previousIndex, event.currentIndex);
      tache.statut = statut;
    }
    this.colonnes.set(c);

    this.tacheService.deplacer(tache.id, statut, event.currentIndex).subscribe({
      next: () => {
        if (statut === 'TERMINEE' && event.previousContainer !== event.container) {
          this.toast.success(`« ${tache.titre} » terminée 🎉`);
        }
        this.rafraichir();
      },
      error: (e) => {
        this.toast.apiError(e, 'Impossible de déplacer la tâche.');
        this.rafraichir();
      },
    });
  }

  /** Recharge tâches + projet (avancement, santé) */
  private rafraichir() {
    const id = Number(this.id());
    forkJoin({ projet: this.projetService.get(id), taches: this.tacheService.list(id) }).subscribe({
      next: ({ projet, taches }) => {
        this.projet.set(projet);
        this.repartir(taches);
      },
    });
  }

  private copie(): Colonnes {
    const c = this.colonnes();
    return { A_FAIRE: [...c.A_FAIRE], EN_COURS: [...c.EN_COURS], TERMINEE: [...c.TERMINEE] };
  }

  // ---------- Fenêtre tâche ----------

  openCreate(statut: StatutTache = 'A_FAIRE') {
    this.editing.set(null);
    this.form.reset({ titre: '', description: '', statut, priorite: 'MOYENNE', echeance: '', assigneId: null });
    this.submitted.set(false);
    this.modalOpen.set(true);
  }

  openEdit(t: Tache) {
    this.editing.set(t);
    this.form.reset({
      titre: t.titre, description: t.description ?? '', statut: t.statut, priorite: t.priorite,
      echeance: t.echeance ?? '', assigneId: t.assigne?.id ?? null,
    });
    this.submitted.set(false);
    this.modalOpen.set(true);
  }

  invalid(name: 'titre') {
    return this.submitted() && this.form.controls[name].invalid;
  }

  save() {
    this.submitted.set(true);
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const data = { ...v, echeance: v.echeance || null, assigneId: v.assigneId ? Number(v.assigneId) : null };
    const current = this.editing();
    const req = current ? this.tacheService.update(current.id, data) : this.tacheService.create(Number(this.id()), data);
    req.subscribe({
      next: (t) => {
        this.saving.set(false);
        this.modalOpen.set(false);
        this.toast.success(current ? `Tâche « ${t.titre} » modifiée.` : `Tâche « ${t.titre} » ajoutée.`);
        this.rafraichir();
      },
      error: (e) => {
        this.saving.set(false);
        this.toast.apiError(e);
      },
    });
  }

  remove(t: Tache) {
    if (!confirm(`Supprimer la tâche « ${t.titre} » ?`)) return;
    this.tacheService.delete(t.id).subscribe({
      next: () => {
        this.modalOpen.set(false);
        this.toast.success('Tâche supprimée.');
        this.rafraichir();
      },
      error: (e) => this.toast.apiError(e),
    });
  }

  // ---------- Affichage ----------

  /** Jauge circulaire du score de santé (CSS conic-gradient) */
  jauge(p: Projet) {
    const couleur = { SAIN: '#2dce89', A_SURVEILLER: '#fb6340', CRITIQUE: '#f5365c' }[p.sante.niveau];
    return `conic-gradient(${couleur} ${p.sante.score * 3.6}deg, #e9ecef 0deg)`;
  }

  couleurAvancement(p: Projet) {
    if (p.enRetard) return 'danger';
    if (p.avancement >= 100) return 'success';
    return p.avancement >= 50 ? 'info' : 'warning';
  }

  /** Jours restants avant l'échéance (négatif = dépassée) */
  joursRestants(date: string | null | undefined): number | null {
    if (!date) return null;
    const ms = new Date(date).setHours(0, 0, 0, 0) - new Date().setHours(0, 0, 0, 0);
    return Math.round(ms / 86_400_000);
  }
}
