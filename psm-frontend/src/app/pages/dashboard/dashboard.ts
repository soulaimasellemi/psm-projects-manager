import { Component, ElementRef, OnDestroy, OnInit, inject, signal, viewChild } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { StatsService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { NIVEAUX_SANTE, STATUTS, Stats, statutInfo } from '../../core/models';

Chart.register(...registerables);

const COULEURS: Record<string, string> = { warning: '#fb6340', info: '#11cdef', success: '#2dce89' };

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DecimalPipe, RouterLink],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit, OnDestroy {
  private statsService = inject(StatsService);
  private toast = inject(ToastService);
  auth = inject(AuthService);

  readonly stats = signal<Stats | null>(null);
  readonly loading = signal(true);
  readonly statutInfo = statutInfo;
  readonly niveaux = NIVEAUX_SANTE;

  private statutCanvas = viewChild.required<ElementRef<HTMLCanvasElement>>('statutCanvas');
  private budgetCanvas = viewChild.required<ElementRef<HTMLCanvasElement>>('budgetCanvas');
  private avancementCanvas = viewChild.required<ElementRef<HTMLCanvasElement>>('avancementCanvas');
  private charts: Chart[] = [];

  ngOnInit() {
    this.statsService.get().subscribe({
      next: (s) => {
        this.stats.set(s);
        this.loading.set(false);
        this.buildCharts(s);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.apiError(e, 'Impossible de charger les statistiques.');
      },
    });
  }

  ngOnDestroy() {
    this.charts.forEach((c) => c.destroy());
  }

  /** Pourcentage de projets terminés */
  tauxTermines(s: Stats) {
    return s.nbProjets ? Math.round(((s.projetsParStatut.TERMINE ?? 0) / s.nbProjets) * 100) : 0;
  }

  private buildCharts(s: Stats) {
    this.charts.forEach((c) => c.destroy());
    Chart.defaults.font.family = "'Open Sans', sans-serif";

    // Répartition des projets par statut (doughnut)
    const doughnut = new Chart(this.statutCanvas().nativeElement, {
      type: 'doughnut',
      data: {
        labels: STATUTS.map((x) => x.label),
        datasets: [{
          data: STATUTS.map((x) => s.projetsParStatut[x.value] ?? 0),
          backgroundColor: STATUTS.map((x) => COULEURS[x.color]),
          borderWidth: 0,
          hoverOffset: 8,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '70%',
        plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, padding: 18 } } },
      },
    });

    // Top clients par budget (barres, carte sombre style Argon)
    const bar = new Chart(this.budgetCanvas().nativeElement, {
      type: 'bar',
      data: {
        labels: s.topClients.map((c) => c.nom),
        datasets: [{
          label: 'Budget (TND)',
          data: s.topClients.map((c) => c.budget),
          backgroundColor: '#5e72e4',
          hoverBackgroundColor: '#11cdef',
          borderRadius: 6,
          maxBarThickness: 34,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: { label: (ctx) => ` ${Number(ctx.parsed.y).toLocaleString('fr-FR')} TND` },
          },
        },
        scales: {
          x: { grid: { display: false }, ticks: { color: '#8898aa' } },
          y: {
            grid: { color: 'rgba(255,255,255,.08)' },
            border: { display: false },
            ticks: { color: '#8898aa', callback: (v) => `${Number(v) / 1000}k` },
          },
        },
      },
    });

    // Avancement des projets en cours (barres horizontales, rouge = en retard)
    const avancement = new Chart(this.avancementCanvas().nativeElement, {
      type: 'bar',
      data: {
        labels: s.avancements.map((a) => (a.libelle.length > 28 ? a.libelle.slice(0, 27) + '…' : a.libelle)),
        datasets: [{
          label: 'Avancement (%)',
          data: s.avancements.map((a) => a.avancement),
          backgroundColor: s.avancements.map((a) => (a.enRetard ? '#f5365c' : '#5e72e4')),
          borderRadius: 6,
          maxBarThickness: 22,
        }],
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const a = s.avancements[ctx.dataIndex]!;
                return ` ${a.avancement} %${a.enRetard ? ' (en retard)' : ''}`;
              },
            },
          },
        },
        scales: {
          x: { min: 0, max: 100, ticks: { color: '#8898aa', callback: (v) => `${v}%` }, grid: { color: '#e9ecef' } },
          y: { grid: { display: false }, ticks: { color: '#525f7f' } },
        },
      },
    });

    this.charts = [doughnut, bar, avancement];
  }
}
