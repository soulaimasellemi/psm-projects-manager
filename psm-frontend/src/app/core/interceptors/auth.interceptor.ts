import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

/**
 * Interceptor HTTP :
 *  1. ajoute "Authorization: Bearer <token>" à chaque appel de l'API
 *  2. 401 -> session expirée : déconnexion + retour au login
 *  3. 403 -> message "accès refusé"
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const toast = inject(ToastService);

  const isApi = req.url.startsWith(environment.apiUrl);
  const isAuthCall = req.url.includes('/auth/');
  const token = auth.token;

  if (token && isApi && !isAuthCall) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (isApi && !isAuthCall) {
        if (err.status === 401) {
          toast.error('Votre session a expiré, veuillez vous reconnecter.');
          auth.logout();
        } else if (err.status === 403) {
          toast.error("Accès refusé : vous n'avez pas les droits nécessaires.");
        } else if (err.status === 0) {
          toast.error('Serveur injoignable. Le backend Spring Boot est-il démarré ?');
        }
      }
      return throwError(() => err);
    }),
  );
};
