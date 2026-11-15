import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ClientService, ProjetService, UserService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import {
  ClientOption, ExportFormat, Membre, NIVEAUX_SANTE, Page, Projet, ProjetFiltres, STATUTS, StatutProjet, initiales,
  statutInfo,
} from '../../core/models';
import { HasRole } from '../../shared/has-role.directive';
import { Pagination } from '../../shared/pagination/pagination';

@Component({
  selector: 'app-projets',
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe, RouterLink, HasRole, Pagination],
  templateUrl: './projets.html',
})
export class Projets implements OnInit {
  private service = inject(ProjetService);
  private clientService = inject(ClientService);
  private userService = inject(UserService);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);
  auth = inject(AuthService);

  readonly statuts = STATUTS;
  readonly statutInfo = statutInfo;
  readonly initiales = initiales;
  readonly niveaux = NIVEAUX_SANTE;
  readonly size = 8;

  readonly data = signal<Page<Projet> | null>(null);
  readonly clients = signal<ClientOption[]>([]);
  readonly collaborateurs = signal<Membre[]>([]);
  readonly loading = signal(false);
  readonly modalOpen = signal(false);
  readonly editing = signal<Projet | null>(null);
  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly exporting = signal<ExportFormat | null>(null);
  readonly membresSelection = signal<number[]>([]);

  // Filtres
  readonly statut = signal<StatutProjet | ''>('');
  readonly clientId = signal<number | null>(null);
  readonly mesProjets = signal(false);
  readonly enRetard = signal(false);
  private q = '';
  private page = 0;
  private searchTimer?: ReturnType<typeof setTimeout>;

  form = this.fb.group({
    libelle: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(150)]),
    idClient: this.fb.control<number | null>(null, Validators.required),
    budget: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    duree: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    statut: this.fb.nonNullable.control<StatutProjet>('EN_ATTENTE'),
    description: this.fb.nonNullable.control('', Validators.maxLength(2000)),
    dateDebut: this.fb.nonNullable.control(''),
    dateFinPrevue: this.fb.nonNullable.control(''),
    avancement: this.fb.nonNullable.control(0, [Validators.min(0), Validators.max(100)]),
  });

  ngOnInit() {
    // Filtres passés en paramètre : /projets?clientId=3 ou /projets?enRetard=true ou /projets?mesProjets=true
    const qp = this.route.snapshot.queryParamMap;
    const id = Number(qp.get('clientId'));
    if (id) this.clientId.set(id);
    if (qp.get('enRetard') === 'true') this.enRetard.set(true);
    if (qp.get('mesProjets') === 'true') this.mesProjets.set(true);

    this.clientService.options().subscribe({ next: (c) => this.clients.set(c) });
    this.userService.options().subscribe({ next: (u) => this.collaborateurs.set(u) });
    this.load();
  }

  /** Filtres courants (utilisés pour la liste ET pour l'export) */
  private filtres(): ProjetFiltres {
    return {
      q: this.q,
      statut: this.statut(),
      clientId: this.clientId(),
      membreId: this.mesProjets() ? (this.auth.user()?.id ?? null) : null,
      enRetard: this.enRetard(),
    };
  }

  load() {
    this.loading.set(true);
    this.service.list(this.filtres(), this.page, this.size).subscribe({
      next: (p) => {
        this.data.set(p);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.apiError(e, 'Impossible de charger les projets.');
      },
    });
  }

  onSearch(value: string) {
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      this.q = value;
      this.page = 0;
      this.load();
    }, 350);
  }

  onStatut(value: string) {
    this.statut.set(value as StatutProjet | '');
    this.reload();
  }

  onClient(value: string) {
    this.clientId.set(value ? Number(value) : null);
    this.reload();
  }

  toggleMesProjets() {
    this.mesProjets.update((v) => !v);
    this.reload();
  }

  toggleEnRetard() {
    this.enRetard.update((v) => !v);
    this.reload();
  }

  private reload() {
    this.page = 0;
    this.load();
  }

  nomClientFiltre() {
    return this.clients().find((c) => c.id === this.clientId())?.nom;
  }

  goTo(p: number) {
    this.page = p;
    this.load();
  }

  exporter(format: ExportFormat) {
    this.exporting.set(format);
    this.service.export(format, this.filtres()).subscribe({
      next: () => {
        this.exporting.set(null);
        this.toast.success(`Export ${format === 'pdf' ? 'PDF' : 'Excel'} téléchargé.`);
      },
      error: (e) => {
        this.exporting.set(null);
        this.toast.apiError(e, "Impossible d'exporter les projets.");
      },
    });
  }

  /** Couleur de la barre d'avancement */
  couleurAvancement(p: Projet) {
    if (p.enRetard) return 'danger';
    if (p.avancement >= 100) return 'success';
    return p.avancement >= 50 ? 'info' : 'warning';
  }

  // ---------- Formulaire ----------

  openCreate() {
    this.editing.set(null);
    this.form.reset({
      libelle: '', idClient: this.clientId(), budget: null, duree: null, statut: 'EN_ATTENTE', description: '',
      dateDebut: '', dateFinPrevue: '', avancement: 0,
    });
    // Le créateur fait partie de l'équipe par défaut
    const me = this.auth.user()?.id;
    this.membresSelection.set(me ? [me] : []);
    this.submitted.set(false);
    this.modalOpen.set(true);
  }

  openEdit(p: Projet) {
    this.editing.set(p);
    this.form.reset({
      libelle: p.libelle, idClient: p.idClient, budget: p.budget, duree: p.duree,
      statut: p.statut, description: p.description ?? '',
      dateDebut: p.dateDebut ?? '', dateFinPrevue: p.dateFinPrevue ?? '', avancement: p.avancement,
    });
    this.membresSelection.set(p.membres.map((m) => m.id));
    this.submitted.set(false);
    this.modalOpen.set(true);
  }

  close() {
    this.modalOpen.set(false);
  }

  toggleMembre(id: number) {
    this.membresSelection.update((l) => (l.includes(id) ? l.filter((x) => x !== id) : [...l, id]));
  }

  /** Collaborateurs proposés : comptes actifs + membres déjà affectés (même désactivés depuis) */
  collaborateursProposes(): Membre[] {
    const actifs = this.collaborateurs();
    const anciens = (this.editing()?.membres ?? []).filter((m) => !actifs.some((a) => a.id === m.id));
    return [...actifs, ...anciens];
  }

  /** Remplit la fin prévue = début + durée (si elle est vide) */
  suggererFin() {
    const { dateDebut, duree, dateFinPrevue } = this.form.getRawValue();
    if (!dateDebut || !duree || dateFinPrevue) return;
    const d = new Date(dateDebut);
    d.setMonth(d.getMonth() + Number(duree));
    this.form.controls.dateFinPrevue.setValue(d.toISOString().slice(0, 10));
  }

  datesIncoherentes() {
    const { dateDebut, dateFinPrevue } = this.form.getRawValue();
    return !!dateDebut && !!dateFinPrevue && dateFinPrevue < dateDebut;
  }

  invalid(name: 'libelle' | 'idClient' | 'budget' | 'duree' | 'avancement') {
    return this.submitted() && this.form.controls[name].invalid;
  }

  save() {
    this.submitted.set(true);
    if (this.form.invalid || this.datesIncoherentes()) return;
    this.saving.set(true);

    const v = this.form.getRawValue();
    const data = {
      libelle: v.libelle,
      idClient: Number(v.idClient),
      budget: Number(v.budget),
      duree: Number(v.duree),
      statut: v.statut,
      description: v.description,
      dateDebut: v.dateDebut || null,
      dateFinPrevue: v.dateFinPrevue || null,
      avancement: v.statut === 'TERMINE' ? 100 : Number(v.avancement),
      membresIds: this.membresSelection(),
    };
    const current = this.editing();
    const request = current ? this.service.update(current.id, data) : this.service.create(data);

    request.subscribe({
      next: (p) => {
        this.saving.set(false);
        this.close();
        this.toast.success(current ? `Projet « ${p.libelle} » modifié.` : `Projet « ${p.libelle} » créé.`);
        if (!current) this.page = 0;
        this.load();
      },
      error: (e) => {
        this.saving.set(false);
        this.toast.apiError(e);
      },
    });
  }

  remove(p: Projet) {
    if (!confirm(`Supprimer le projet « ${p.libelle} » ?`)) return;
    this.service.delete(p.id).subscribe({
      next: () => {
        this.toast.success(`Projet « ${p.libelle} » supprimé.`);
        if (this.data()?.content.length === 1 && this.page > 0) this.page--;
        this.load();
      },
      error: (e) => this.toast.apiError(e),
    });
  }
}
