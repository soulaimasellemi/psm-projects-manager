import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  template: `
    @if (totalElements() > 0) {
      <div class="card-footer d-flex flex-wrap gap-2 justify-content-between align-items-center py-3">
        <small class="text-muted">
          {{ from() }}–{{ to() }} sur <strong>{{ totalElements() }}</strong> {{ label() }}
        </small>
        @if (totalPages() > 1) {
          <ul class="pagination pagination-psm mb-0">
            <li class="page-item" [class.disabled]="page() === 0">
              <button class="page-link" type="button" (click)="go(page() - 1)" aria-label="Précédent">
                <i class="fas fa-angle-left"></i>
              </button>
            </li>
            @for (p of pages(); track p) {
              <li class="page-item" [class.active]="p === page()">
                <button class="page-link" type="button" (click)="go(p)">{{ p + 1 }}</button>
              </li>
            }
            <li class="page-item" [class.disabled]="page() >= totalPages() - 1">
              <button class="page-link" type="button" (click)="go(page() + 1)" aria-label="Suivant">
                <i class="fas fa-angle-right"></i>
              </button>
            </li>
          </ul>
        }
      </div>
    }
  `,
})
export class Pagination {
  readonly page = input.required<number>();
  readonly size = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly totalElements = input.required<number>();
  readonly label = input('élément(s)');
  readonly pageChange = output<number>();

  readonly pages = computed(() => Array.from({ length: this.totalPages() }, (_, i) => i));
  readonly from = computed(() => this.page() * this.size() + 1);
  readonly to = computed(() => Math.min((this.page() + 1) * this.size(), this.totalElements()));

  go(p: number) {
    if (p >= 0 && p < this.totalPages() && p !== this.page()) this.pageChange.emit(p);
  }
}
