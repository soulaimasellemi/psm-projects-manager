import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/guards/auth.guard';
import { AdminLayout } from './layout/admin-layout/admin-layout';

export const routes: Routes = [
  // Pages publiques (redirigent vers le dashboard si déjà connecté)
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Connexion | PSM',
    loadComponent: () => import('./pages/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    title: 'Inscription | PSM',
    loadComponent: () => import('./pages/auth/register/register').then((m) => m.Register),
  },

  // Espace connecté (AuthGuard)
  {
    path: '',
    component: AdminLayout,
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Tableau de bord | PSM',
        data: { title: 'Tableau de bord' },
        loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'clients',
        title: 'Clients | PSM',
        data: { title: 'Clients' },
        loadComponent: () => import('./pages/clients/clients').then((m) => m.Clients),
      },
      {
        path: 'projets',
        title: 'Projets | PSM',
        data: { title: 'Projets' },
        loadComponent: () => import('./pages/projets/projets').then((m) => m.Projets),
      },
      {
        path: 'utilisateurs',
        title: 'Utilisateurs | PSM',
        canActivate: [roleGuard],
        data: { title: 'Utilisateurs', roles: ['ADMIN'] }, // RoleGuard : admin uniquement
        loadComponent: () => import('./pages/users/users').then((m) => m.Users),
      },
      {
        path: 'profil',
        title: 'Mon profil | PSM',
        data: { title: 'Mon profil' },
        loadComponent: () => import('./pages/profil/profil').then((m) => m.Profil),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
