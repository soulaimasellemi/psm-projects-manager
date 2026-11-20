import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Preferences, Theme, WidgetPref } from '../models';

/** Blocs du tableau de bord que l'utilisateur peut afficher, masquer et réordonner */
export const WIDGETS_DASHBOARD = [
  { id: 'indicateurs', label: 'Indicateurs clés', description: 'Clients, projets, budget, inscriptions', icon: 'fa-gauge-high' },
  { id: 'graphiques', label: 'Graphiques', description: 'Top clients par budget, projets par statut', icon: 'fa-chart-column' },
  { id: 'risques', label: 'Projets à risque', description: 'Santé du portefeuille et projets à surveiller', icon: 'fa-heart-pulse' },
  { id: 'suivi', label: 'Suivi et avancement', description: 'Retards, avancement moyen, mes projets', icon: 'fa-bars-progress' },
  { id: 'derniers', label: 'Derniers projets', description: 'Les 5 projets les plus récents', icon: 'fa-clock-rotate-left' },
  { id: 'raccourcis', label: 'Raccourcis', description: 'Ajouter un client, lancer un projet', icon: 'fa-bolt' },
] as const;

export type WidgetId = (typeof WIDGETS_DASHBOARD)[number]['id'];

const STORAGE_KEY = 'psm_prefs';

/**
 * Préférences d'affichage : thème clair / sombre et disposition du dashboard.
 *  - appliquées immédiatement et gardées dans le navigateur (affichage instantané au rechargement) ;
 *  - enregistrées sur le compte (PUT /api/users/me/preferences) quand l'utilisateur est connecté,
 *    pour les retrouver sur un autre ordinateur.
 */
@Injectable({ providedIn: 'root' })
export class PreferencesService {
  private http = inject(HttpClient);
  private document = inject(DOCUMENT);

  readonly theme = signal<Theme>('light');
  private readonly _dashboard = signal<WidgetPref[]>(PreferencesService.parDefaut());
  /** Blocs du dashboard dans l'ordre d'affichage (visibles ou non) */
  readonly dashboard = this._dashboard.asReadonly();
  /** Blocs visibles, dans l'ordre */
  readonly blocsVisibles = computed(() => this._dashboard().filter((w) => w.visible).map((w) => w.id as WidgetId));

  /** Renseigné par AuthService : les changements ne sont envoyés au serveur que si l'on est connecté */
  connecte = false;
  private saveTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    const locales = this.lireLocal();
    if (locales) {
      this.appliquer(locales);
    } else {
      // Premier lancement : on suit le thème de l'ordinateur
      const sombre = this.document.defaultView?.matchMedia?.('(prefers-color-scheme: dark)').matches;
      this.theme.set(sombre ? 'dark' : 'light');
    }
    // Bootstrap 5.3 : data-bs-theme="dark" active le mode sombre natif des composants
    effect(() => this.document.documentElement.setAttribute('data-bs-theme', this.theme()));
  }

  static parDefaut(): WidgetPref[] {
    return WIDGETS_DASHBOARD.map((w) => ({ id: w.id, visible: true }));
  }

  // ---------- Modifications ----------

  basculerTheme() {
    this.theme.update((t) => (t === 'dark' ? 'light' : 'dark'));
    this.enregistrer();
  }

  definirDashboard(blocs: WidgetPref[]) {
    this._dashboard.set(this.normaliser(blocs));
    this.enregistrer();
  }

  reinitialiserDashboard() {
    this.definirDashboard(PreferencesService.parDefaut());
  }

  /** Préférences reçues du serveur à la connexion : elles priment sur celles du navigateur */
  chargerDuCompte(prefs: Preferences | null | undefined) {
    if (prefs) {
      this.appliquer(prefs);
      this.sauverLocal();
    } else {
      // Compte sans préférences : on lui enregistre celles du navigateur
      this.enregistrer();
    }
  }

  // ---------- Interne ----------

  private appliquer(p: Partial<Preferences>) {
    if (p.theme === 'dark' || p.theme === 'light') this.theme.set(p.theme);
    if (Array.isArray(p.dashboard)) this._dashboard.set(this.normaliser(p.dashboard));
  }

  /** Ignore les blocs inconnus, ajoute (visibles) les nouveaux blocs absents des préférences */
  private normaliser(blocs: WidgetPref[]): WidgetPref[] {
    const connus = new Set<string>(WIDGETS_DASHBOARD.map((w) => w.id));
    const resultat = blocs.filter((b) => connus.has(b.id)).map((b) => ({ id: b.id, visible: !!b.visible }));
    for (const w of WIDGETS_DASHBOARD) {
      if (!resultat.some((b) => b.id === w.id)) resultat.push({ id: w.id, visible: true });
    }
    return resultat;
  }

  private courantes(): Preferences {
    return { theme: this.theme(), dashboard: this._dashboard() };
  }

  private enregistrer() {
    this.sauverLocal();
    if (!this.connecte) return;
    clearTimeout(this.saveTimer);
    this.saveTimer = setTimeout(() => {
      this.http.put(`${environment.apiUrl}/users/me/preferences`, this.courantes()).subscribe({
        error: () => {
          /* préférences d'affichage : un échec n'empêche pas d'utiliser l'application */
        },
      });
    }, 400);
  }

  private sauverLocal() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.courantes()));
    } catch {
      /* stockage indisponible (navigation privée...) : on garde seulement la session */
    }
  }

  private lireLocal(): Preferences | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as Preferences) : null;
    } catch {
      return null;
    }
  }
}
