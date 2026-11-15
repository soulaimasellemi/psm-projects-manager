import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Page, RoleName, User, initiales } from '../../core/models';
import { Pagination } from '../../shared/pagination/pagination';

/** Gestion des utilisateurs (page réservée à l'ADMIN via RoleGuard) */
@Component({
  selector: 'app-users',
  imports: [DatePipe, ReactiveFormsModule, Pagination],
  templateUrl: './users.html',
})
export class Users implements OnInit {
  private service = inject(UserService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);
  auth = inject(AuthService);

  readonly initiales = initiales;
  readonly allRoles: RoleName[] = ['ADMIN', 'COLLABORATEUR'];
  readonly size = 8;

  readonly data = signal<Page<User> | null>(null);
  readonly loading = signal(false);
  readonly filtre = signal<'tous' | 'attente' | 'actifs'>('tous');
  readonly rolesUser = signal<User | null>(null);
  readonly rolesSelection = signal<RoleName[]>([]);

  // Création d'un compte (POST /api/users)
  readonly createOpen = signal(false);
  readonly createSubmitted = signal(false);
  readonly creating = signal(false);
  readonly createRoles = signal<RoleName[]>(['COLLABORATEUR']);
  readonly showPassword = signal(false);
  createForm = this.fb.nonNullable.group({
    prenom: ['', [Validators.required, Validators.maxLength(80)]],
    nom: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email]],
    tel: ['', Validators.maxLength(30)],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  private q = '';
  private page = 0;
  private searchTimer?: ReturnType<typeof setTimeout>;

  ngOnInit() {
    this.load();
  }

  load() {
    const f = this.filtre();
    const enabled = f === 'tous' ? null : f === 'actifs';
    this.loading.set(true);
    this.service.list(this.q, enabled, this.page, this.size).subscribe({
      next: (p) => {
        this.data.set(p);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.apiError(e, 'Impossible de charger les utilisateurs.');
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

  setFiltre(f: 'tous' | 'attente' | 'actifs') {
    this.filtre.set(f);
    this.page = 0;
    this.load();
  }

  goTo(p: number) {
    this.page = p;
    this.load();
  }

  isMe(u: User) {
    return u.id === this.auth.user()?.id;
  }

  toggleStatut(u: User) {
    const enabled = !u.enabled;
    this.service.setStatut(u.id, enabled).subscribe({
      next: () => {
        this.toast.success(enabled
          ? `Compte de ${u.prenom} ${u.nom} activé (un email lui a été envoyé).`
          : `Compte de ${u.prenom} ${u.nom} désactivé.`);
        this.load();
      },
      error: (e) => this.toast.apiError(e),
    });
  }

  openRoles(u: User) {
    this.rolesUser.set(u);
    this.rolesSelection.set([...u.roles]);
  }

  toggleRole(r: RoleName) {
    this.rolesSelection.update((list) => (list.includes(r) ? list.filter((x) => x !== r) : [...list, r]));
  }

  saveRoles() {
    const u = this.rolesUser();
    const roles = this.rolesSelection();
    if (!u || roles.length === 0) return;
    this.service.setRoles(u.id, roles).subscribe({
      next: () => {
        this.toast.success(`Rôles de ${u.prenom} ${u.nom} mis à jour.`);
        this.rolesUser.set(null);
        this.load();
      },
      error: (e) => this.toast.apiError(e),
    });
  }

  openCreate() {
    this.createForm.reset();
    this.createRoles.set(['COLLABORATEUR']);
    this.createSubmitted.set(false);
    this.showPassword.set(false);
    this.createOpen.set(true);
  }

  toggleCreateRole(r: RoleName) {
    this.createRoles.update((list) => (list.includes(r) ? list.filter((x) => x !== r) : [...list, r]));
  }

  createInvalid(name: 'prenom' | 'nom' | 'email' | 'tel' | 'password') {
    return this.createSubmitted() && this.createForm.controls[name].invalid;
  }

  create() {
    this.createSubmitted.set(true);
    if (this.createForm.invalid || this.createRoles().length === 0) return;
    this.creating.set(true);
    this.service.create({ ...this.createForm.getRawValue(), roles: this.createRoles() }).subscribe({
      next: (u) => {
        this.creating.set(false);
        this.createOpen.set(false);
        const role = u.roles.includes('ADMIN') ? 'administrateur' : 'collaborateur';
        this.toast.success(`Compte ${role} de ${u.prenom} ${u.nom} créé (actif).`);
        this.page = 0;
        this.load();
      },
      error: (e) => {
        this.creating.set(false);
        this.toast.apiError(e);
      },
    });
  }

  remove(u: User) {
    if (!confirm(`Supprimer définitivement le compte de ${u.prenom} ${u.nom} ?`)) return;
    this.service.delete(u.id).subscribe({
      next: () => {
        this.toast.success('Utilisateur supprimé.');
        if (this.data()?.content.length === 1 && this.page > 0) this.page--;
        this.load();
      },
      error: (e) => this.toast.apiError(e),
    });
  }
}
