import { Component, inject, input } from '@angular/core';
import { PreferencesService } from '../../core/services/preferences.service';

/** Bouton lune / soleil : bascule entre le thème clair et le thème sombre */
@Component({
  selector: 'app-theme-toggle',
  template: `
    <button type="button" class="theme-toggle" [class.theme-toggle-floating]="floating()"
            (click)="prefs.basculerTheme()"
            [title]="prefs.theme() === 'dark' ? 'Passer en mode clair' : 'Passer en mode sombre'"
            [attr.aria-label]="prefs.theme() === 'dark' ? 'Passer en mode clair' : 'Passer en mode sombre'">
      <i class="fas" [class.fa-moon]="prefs.theme() === 'light'" [class.fa-sun]="prefs.theme() === 'dark'"></i>
    </button>
  `,
})
export class ThemeToggle {
  readonly prefs = inject(PreferencesService);
  /** true sur les pages de connexion (bouton fixé en haut à droite) */
  readonly floating = input(false);
}
