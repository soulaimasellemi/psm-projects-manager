import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActiviteService } from '../../core/services/api.services';
import { ToastService } from '../../core/services/toast.service';
import { ACTIONS, ActionActivite, Activite, CIBLES, CibleActivite, Page, initiales } from '../../core/models';
import { Pagination } from '../../shared/pagination/pagination';

/** Historique des actions (journal d'audit) : page réservée à l'ADMIN via RoleGuard */
@Component({
  selector: 'app-historique',
  imports: [DatePipe, Pagination],
  templateUrl: './historique.html',
})
export class Historique implements OnInit {
  private service = inject(ActiviteService);
  private toast = inject(ToastService);

  readonly actions = ACTIONS;
  readonly cibles = CIBLES;
  readonly listeActions = Object.keys(ACTIONS) as ActionActivite[];
  readonly listeCibles = Object.keys(CIBLES) as CibleActivite[];
  readonly initiales = initiales;
  readonly size = 15;

  readonly data = signal<Page<Activite> | null>(null);
  readonly loading = signal(false);
  readonly cible = signal<CibleActivite | ''>('');
  readonly action = signal<ActionActivite | ''>('');

  private q = '';
  private page = 0;
  private searchTimer?: ReturnType<typeof setTimeout>;

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.service.list(this.q, this.cible(), this.action(), this.page, this.size).subscribe({
      next: (p) => {
        this.data.set(p);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.apiError(e, "Impossible de charger l'historique.");
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

  onCible(value: string) {
    this.cible.set(value as CibleActivite | '');
    this.page = 0;
    this.load();
  }

  onAction(value: string) {
    this.action.set(value as ActionActivite | '');
    this.page = 0;
    this.load();
  }

  goTo(p: number) {
    this.page = p;
    this.load();
  }
}
