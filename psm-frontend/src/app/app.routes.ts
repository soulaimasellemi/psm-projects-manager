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
  {
    path: 'mot-de-passe-oublie',
    canActivate: [guestGuard],
    title: 'Mot de passe oublié | PSM',
    loadComponent: () => import('./pages/auth/forgot-password/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    // Lien reçu par email : /reinitialiser-mot-de-passe?token=...
    path: 'reinitialiser-mot-de-passe',
    title: 'Nouveau mot de passe | PSM',
    loadComponent: () => import('./pages/auth/reset-password/reset-password').then((m) => m.ResetPassword),
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
        // Détail d'un projet : santé + tableau Kanban des tâches
        path: 'projets/:id',
        title: 'Projet | PSM',
        data: { title: 'Détail du projet' },
        loadComponent: () => import('./pages/projet-detail/projet-detail').then((m) => m.ProjetDetail),
      },
      {
        path: 'utilisateurs',
        title: 'Utilisateurs | PSM',
        canActivate: [roleGuard],
        data: { title: 'Utilisateurs', roles: ['ADMIN'] }, // RoleGuard : admin uniquement
        loadComponent: () => import('./pages/users/users').then((m) => m.Users),
      },
      {
        path: 'historique',
        title: 'Historique | PSM',
        canActivate: [roleGuard],
        data: { title: 'Historique des actions', roles: ['ADMIN'] }, // RoleGuard : admin uniquement
        loadComponent: () => import('./pages/historique/historique').then((m) => m.Historique),
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
