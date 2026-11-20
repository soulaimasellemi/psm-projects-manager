import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { PreferencesService } from './core/services/preferences.service';
import { Toasts } from './shared/toasts/toasts';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toasts],
  template: `<router-outlet /><app-toasts />`,
})
export class App {
  // Applique le thème (clair / sombre) dès le démarrage, sur toutes les pages
  private readonly prefs = inject(PreferencesService);
}
