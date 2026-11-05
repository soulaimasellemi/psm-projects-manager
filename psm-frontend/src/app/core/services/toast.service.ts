import { Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

export interface Toast {
  id: number;
  type: 'success' | 'danger' | 'info' | 'warning';
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  readonly toasts = signal<Toast[]>([]);

  success(message: string) { this.show('success', message); }
  error(message: string) { this.show('danger', message); }
  info(message: string) { this.show('info', message); }

  /**
   * Affiche le message d'erreur renvoyé par l'API.
   * Les erreurs 0 / 401 / 403 sont déjà signalées par l'interceptor.
   */
  apiError(err: unknown, fallback = 'Une erreur est survenue.') {
    if (err instanceof HttpErrorResponse && [0, 401, 403].includes(err.status)) return;
    this.error(apiMessage(err, fallback));
  }

  show(type: Toast['type'], message: string) {
    const id = this.nextId++;
    this.toasts.update((list) => [...list, { id, type, message }]);
    setTimeout(() => this.dismiss(id), 4000);
  }

  dismiss(id: number) {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }
}

/** Extrait le message lisible d'une erreur HTTP de l'API Spring. */
export function apiMessage(err: unknown, fallback = 'Une erreur est survenue.'): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as { message?: string; errors?: Record<string, string> } | null;
    if (body?.errors) {
      const first = Object.values(body.errors)[0];
      if (first) return first;
    }
    if (body?.message) return body.message;
    if (err.status === 0) return 'Serveur injoignable. Le backend Spring Boot est-il démarré ?';
  }
  return fallback;
}
