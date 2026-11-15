import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClientService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Client, ExportFormat, Page, SECTEURS, initiales } from '../../core/models';
import { HasRole } from '../../shared/has-role.directive';
import { Pagination } from '../../shared/pagination/pagination';

@Component({
  selector: 'app-clients',
  imports: [ReactiveFormsModule, CurrencyPipe, RouterLink, HasRole, Pagination],
  templateUrl: './clients.html',
})
export class Clients implements OnInit {
  private service = inject(ClientService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);
  auth = inject(AuthService);

  readonly secteurs = SECTEURS;
  readonly initiales = initiales;
  readonly size = 8;

  readonly data = signal<Page<Client> | null>(null);
  readonly loading = signal(false);
  readonly modalOpen = signal(false);
  readonly editing = signal<Client | null>(null);
  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly exporting = signal<ExportFormat | null>(null);

  private q = '';
  private page = 0;
  private searchTimer?: ReturnType<typeof setTimeout>;

  form = this.fb.nonNullable.group({
    nom: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    tel: ['', [Validators.maxLength(30)]],
    adresse: ['', [Validators.maxLength(255)]],
    secteur: [''],
  });

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.service.list(this.q, this.page, this.size).subscribe({
      next: (p) => {
        this.data.set(p);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.apiError(e, 'Impossible de charger les clients.');
      },
    });
  }

  /** Recherche avec un léger délai (debounce) */
  onSearch(value: string) {
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      this.q = value;
      this.page = 0;
      this.load();
    }, 350);
  }

  /** Export Excel / PDF de la liste (avec la recherche en cours) */
  exporter(format: ExportFormat) {
    this.exporting.set(format);
    this.service.export(format, this.q).subscribe({
      next: () => {
        this.exporting.set(null);
        this.toast.success(`Export ${format === 'pdf' ? 'PDF' : 'Excel'} téléchargé.`);
      },
      error: (e) => {
        this.exporting.set(null);
        this.toast.apiError(e, "Impossible d'exporter les clients.");
      },
    });
  }

  goTo(p: number) {
    this.page = p;
    this.load();
  }

  openCreate() {
    this.editing.set(null);
    this.form.reset();
    this.submitted.set(false);
    this.modalOpen.set(true);
  }

  openEdit(c: Client) {
    this.editing.set(c);
    this.form.reset({
      nom: c.nom,
      email: c.email ?? '',
      tel: c.tel ?? '',
      adresse: c.adresse ?? '',
      secteur: c.secteur ?? '',
    });
    this.submitted.set(false);
    this.modalOpen.set(true);
  }

  close() {
    this.modalOpen.set(false);
  }

  invalid(name: 'nom' | 'email' | 'tel' | 'adresse') {
    return this.submitted() && this.form.controls[name].invalid;
  }

  save() {
    this.submitted.set(true);
    if (this.form.invalid) return;
    this.saving.set(true);

    const data = this.form.getRawValue();
    const current = this.editing();
    const request = current ? this.service.update(current.id, data) : this.service.create(data);

    request.subscribe({
      next: (c) => {
        this.saving.set(false);
        this.close();
        this.toast.success(current ? `Client « ${c.nom} » modifié.` : `Client « ${c.nom} » ajouté.`);
        if (!current) this.page = 0;
        this.load();
      },
      error: (e) => {
        this.saving.set(false);
        this.toast.apiError(e);
      },
    });
  }

  remove(c: Client) {
    const msg = c.nbProjets
      ? `Supprimer « ${c.nom} » ? Ses ${c.nbProjets} projet(s) seront aussi supprimés.`
      : `Supprimer le client « ${c.nom} » ?`;
    if (!confirm(msg)) return;

    this.service.delete(c.id).subscribe({
      next: () => {
        this.toast.success(`Client « ${c.nom} » supprimé.`);
        if (this.data()?.content.length === 1 && this.page > 0) this.page--;
        this.load();
      },
      error: (e) => this.toast.apiError(e),
    });
  }
}
