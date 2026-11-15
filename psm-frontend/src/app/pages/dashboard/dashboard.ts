import { Component, ElementRef, OnDestroy, OnInit, effect, inject, signal, viewChild } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CdkDrag, CdkDragDrop, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { Chart, registerables } from 'chart.js';
import { StatsService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { PreferencesService, WIDGETS_DASHBOARD } from '../../core/services/preferences.service';
import { ToastService } from '../../core/services/toast.service';
import { NIVEAUX_SANTE, STATUTS, Stats, WidgetPref, statutInfo } from '../../core/models';

Chart.register(...registerables);

const COULEURS: Record<string, string> = { warning: '#fb6340', info: '#11cdef', success: '#2dce89' };

/** Couleurs des axes et légendes selon le thème */
const PALETTES = {
  light: { texte: '#525f7f', discret: '#8898aa', grille: '#e9ecef' },
  dark: { texte: '#c4cbe0', discret: '#8a94b0', grille: 'rgba(255,255,255,.08)' },
};

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DecimalPipe, RouterLink, CdkDropList, CdkDrag],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit, OnDestroy {
  private statsService = inject(StatsService);
  private toast = inject(ToastService);
  auth = inject(AuthService);
  prefs = inject(PreferencesService);

  readonly stats = signal<Stats | null>(null);
  readonly loading = signal(true);
  readonly statutInfo = statutInfo;
  readonly niveaux = NIVEAUX_SANTE;

  // Personnalisation (copie de travail, appliquée à l'enregistrement)
  readonly personnalisation = signal(false);
  readonly brouillon = signal<WidgetPref[]>([]);

  // Les canvas n'existent que si leur bloc est affiché
  private statutCanvas = viewChild<ElementRef<HTMLCanvasElement>>('statutCanvas');
  private budgetCanvas = viewChild<ElementRef<HTMLCanvasElement>>('budgetCanvas');
  private avancementCanvas = viewChild<ElementRef<HTMLCanvasElement>>('avancementCanvas');
  private charts: Chart[] = [];

  constructor() {
    // Redessine les graphiques quand les données, le thème ou la disposition changent
    effect(() => {
      const s = this.stats();
      this.prefs.theme();
      this.prefs.blocsVisibles();
      if (s) setTimeout(() => this.buildCharts(s)); // après le rendu des blocs
    });
  }

  ngOnInit() {
    this.statsService.get().subscribe({
      next: (s) => {
        this.stats.set(s);
        this.loading.set(false);
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

  // ---------- Personnalisation ----------

  infoBloc(id: string) {
    return WIDGETS_DASHBOARD.find((w) => w.id === id) ?? WIDGETS_DASHBOARD[0];
  }

  ouvrirPersonnalisation() {
    this.brouillon.set(this.prefs.dashboard().map((w) => ({ ...w })));
    this.personnalisation.set(true);
  }

  deplacerBloc(event: CdkDragDrop<WidgetPref[]>) {
    const liste = [...this.brouillon()];
    moveItemInArray(liste, event.previousIndex, event.currentIndex);
    this.brouillon.set(liste);
  }

  basculerBloc(index: number) {
    this.brouillon.update((l) => l.map((w, i) => (i === index ? { ...w, visible: !w.visible } : w)));
  }

  reinitialiserBrouillon() {
    this.brouillon.set(PreferencesService.parDefaut());
  }

  enregistrerPersonnalisation() {
    this.prefs.definirDashboard(this.brouillon());
    this.personnalisation.set(false);
    this.toast.success('Tableau de bord personnalisé et enregistré sur votre compte.');
  }

  // ---------- Graphiques ----------

  private buildCharts(s: Stats) {
    this.charts.forEach((c) => c.destroy());
    this.charts = [];
    const p = PALETTES[this.prefs.theme()];
    Chart.defaults.font.family = "'Open Sans', sans-serif";
    Chart.defaults.color = p.texte;

    // Répartition des projets par statut (doughnut)
    const statut = this.statutCanvas()?.nativeElement;
    if (statut) {
      this.charts.push(new Chart(statut, {
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
          plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, padding: 18, color: p.texte } } },
        },
      }));
    }

    // Top clients par budget (barres, carte sombre style Argon)
    const budget = this.budgetCanvas()?.nativeElement;
    if (budget) {
      this.charts.push(new Chart(budget, {
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
      }));
    }

    // Avancement des projets en cours (barres horizontales, rouge = en retard)
    const avancement = this.avancementCanvas()?.nativeElement;
    if (avancement) {
      this.charts.push(new Chart(avancement, {
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
            x: { min: 0, max: 100, ticks: { color: p.discret, callback: (v) => `${v}%` }, grid: { color: p.grille } },
            y: { grid: { display: false }, ticks: { color: p.texte } },
          },
        },
      }));
    }
  }
}
