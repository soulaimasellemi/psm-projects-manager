import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { RoleName, initiales } from '../../core/models';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  color: string;
  roles?: RoleName[];
}

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.html',
})
export class AdminLayout {
  auth = inject(AuthService);
  private router = inject(Router);

  readonly sidebarOpen = signal(false);
  readonly menuOpen = signal(false);
  readonly year = new Date().getFullYear();

  readonly nav: NavItem[] = [
    { path: '/dashboard', label: 'Tableau de bord', icon: 'fa-chart-pie', color: 'text-primary' },
    { path: '/clients', label: 'Clients', icon: 'fa-building', color: 'text-info' },
    { path: '/projets', label: 'Projets', icon: 'fa-diagram-project', color: 'text-orange' },
    { path: '/utilisateurs', label: 'Utilisateurs', icon: 'fa-users-gear', color: 'text-danger', roles: ['ADMIN'] },
    { path: '/historique', label: 'Historique', icon: 'fa-clock-rotate-left', color: 'text-warning', roles: ['ADMIN'] },
    { path: '/profil', label: 'Mon profil', icon: 'fa-user', color: 'text-success' },
  ];

  /** Titre de la page courante (route.data.title) */
  readonly title = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.currentTitle()),
    ),
    { initialValue: this.currentTitle() },
  );

  visible(item: NavItem) {
    return !item.roles || this.auth.hasRole(...item.roles);
  }

  initiales() {
    const u = this.auth.user();
    return initiales(u ? `${u.prenom} ${u.nom}` : '');
  }

  closeMenus() {
    this.sidebarOpen.set(false);
    this.menuOpen.set(false);
  }

  logout() {
    this.closeMenus();
    this.auth.logout();
  }

  private currentTitle(): string {
    let r = this.router.routerState.snapshot.root;
    while (r.firstChild) r = r.firstChild;
    return (r.data['title'] as string | undefined) ?? '';
  }
}
