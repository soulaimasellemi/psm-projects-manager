import { ThemeToggle } from '../../../shared/theme-toggle/theme-toggle';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { apiMessage } from '../../../core/services/toast.service';

/**
 * Lien "Valider le compte" de l'email envoyé à l'admin : /valider-compte?token=...
 * La validation se fait au clic sur le bouton (et non à l'ouverture de la page),
 * pour qu'un logiciel qui prévisualise les liens ne valide pas un compte à votre place.
 */
@Component({
  selector: 'app-validate-account',
  imports: [ThemeToggle, RouterLink],
  templateUrl: './validate-account.html',
})
export class ValidateAccount implements OnInit {
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);

  readonly token = signal<string | null>(null);
  readonly loading = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly connecte = this.auth.isLoggedIn();

  ngOnInit() {
    this.token.set(this.route.snapshot.queryParamMap.get('token'));
  }

  valider() {
    const token = this.token();
    if (!token) return;
    this.loading.set(true);
    this.error.set(null);
    this.auth.validateAccount(token).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.message.set(res.message);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(apiMessage(err, 'Impossible de valider ce compte.'));
      },
    });
  }
}
