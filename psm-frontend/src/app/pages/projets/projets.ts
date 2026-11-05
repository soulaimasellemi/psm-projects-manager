import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ClientService, ProjetService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { ClientOption, Page, Projet, STATUTS, StatutProjet, statutInfo } from '../../core/models';
import { HasRole } from '../../shared/has-role.directive';
import { Pagination } from '../../shared/pagination/pagination';

@Component({
  selector: 'app-projets',
  imports: [ReactiveFormsModule, CurrencyPipe, HasRole, Pagination],
  templateUrl: './projets.html',
})
export class Projets implements OnInit {
  private service = inject(ProjetService);
  private clientService = inject(ClientService);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);
  auth = inject(AuthService);

  readonly statuts = STATUTS;
  readonly statutInfo = statutInfo;
  readonly size = 8;

  readonly data = signal<Page<Projet> | null>(null);
  readonly clients = signal<ClientOption[]>([]);
  readonly loading = signal(false);
  readonly modalOpen = signal(false);
  readonly editing = signal<Projet | null>(null);
  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly detail = signal<Projet | null>(null);

  // Filtres
  readonly statut = signal<StatutProjet | ''>('');
  readonly clientId = signal<number | null>(null);
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
  });

  ngOnInit() {
    // Filtre client passé depuis la page Clients : /projets?clientId=3
    const id = Number(this.route.snapshot.queryParamMap.get('clientId'));
    if (id) this.clientId.set(id);

    this.clientService.options().subscribe({ next: (c) => this.clients.set(c) });
    this.load();
  }

  load() {
    this.loading.set(true);
    this.service.list(this.q, this.statut(), this.clientId(), this.page, this.size).subscribe({
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
    this.page = 0;
    this.load();
  }

  onClient(value: string) {
    this.clientId.set(value ? Number(value) : null);
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

  openCreate() {
    this.editing.set(null);
    this.form.reset({
      libelle: '', idClient: this.clientId(), budget: null, duree: null, statut: 'EN_ATTENTE', description: '',
    });
    this.submitted.set(false);
    this.modalOpen.set(true);
  }

  openEdit(p: Projet) {
    this.editing.set(p);
    this.form.reset({
      libelle: p.libelle, idClient: p.idClient, budget: p.budget, duree: p.duree,
      statut: p.statut, description: p.description ?? '',
    });
    this.submitted.set(false);
    this.modalOpen.set(true);
  }

  close() {
    this.modalOpen.set(false);
  }

  invalid(name: 'libelle' | 'idClient' | 'budget' | 'duree') {
    return this.submitted() && this.form.controls[name].invalid;
  }

  save() {
    this.submitted.set(true);
    if (this.form.invalid) return;
    this.saving.set(true);

    const v = this.form.getRawValue();
    const data = {
      libelle: v.libelle,
      idClient: Number(v.idClient),
      budget: Number(v.budget),
      duree: Number(v.duree),
      statut: v.statut,
      description: v.description,
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
