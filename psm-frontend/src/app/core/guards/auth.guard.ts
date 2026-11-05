import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';
import { RoleName } from '../models';

/** AuthGuard : bloque l'accès si l'utilisateur n'est pas connecté (ou token expiré). */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isLoggedIn()) return true;
  auth.logout(false);
  return inject(Router).createUrlTree(['/login']);
};

/** RoleGuard : vérifie que l'utilisateur possède un des rôles de route.data.roles */
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const roles = (route.data['roles'] ?? []) as RoleName[];
  if (roles.length === 0 || auth.hasRole(...roles)) return true;
  inject(ToastService).error("Accès refusé : cette page est réservée à l'administrateur.");
  return inject(Router).createUrlTree(['/dashboard']);
};

/** GuestGuard : un utilisateur déjà connecté ne revoit pas login/register. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? inject(Router).createUrlTree(['/dashboard']) : true;
};
