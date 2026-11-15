import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService, apiMessage } from '../../../core/services/toast.service';

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const p = group.get('password')?.value;
  const c = group.get('confirm')?.value;
  return p && c && p !== c ? { mismatch: true } : null;
}

/** Mot de passe oublié, étape 2 : lien reçu par email (?token=...) -> nouveau mot de passe */
@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
})
export class ResetPassword implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toast = inject(ToastService);

  readonly token = signal<string | null>(null);
  readonly loading = signal(false);
  readonly submitted = signal(false);
  readonly error = signal<string | null>(null);
  readonly showPassword = signal(false);

  form = this.fb.nonNullable.group(
    {
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirm: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  ngOnInit() {
    this.token.set(this.route.snapshot.queryParamMap.get('token'));
  }

  submit() {
    this.submitted.set(true);
    this.error.set(null);
    const token = this.token();
    if (this.form.invalid || !token) return;

    this.loading.set(true);
    this.auth.resetPassword(token, this.form.getRawValue().password).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.toast.success(res.message);
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(apiMessage(err, 'Impossible de modifier le mot de passe.'));
      },
    });
  }
}
