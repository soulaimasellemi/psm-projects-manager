import { Component, inject } from '@angular/core';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-toasts',
  template: `
    <div class="psm-toasts">
      @for (t of toast.toasts(); track t.id) {
        <div class="psm-toast psm-toast-{{ t.type }}" role="alert">
          <i class="fas" [class.fa-circle-check]="t.type === 'success'"
             [class.fa-circle-exclamation]="t.type === 'danger' || t.type === 'warning'"
             [class.fa-circle-info]="t.type === 'info'"></i>
          <span class="flex-grow-1">{{ t.message }}</span>
          <button type="button" class="btn-close btn-close-white btn-sm" (click)="toast.dismiss(t.id)" aria-label="Fermer"></button>
        </div>
      }
    </div>
  `,
})
export class Toasts {
  toast = inject(ToastService);
}
