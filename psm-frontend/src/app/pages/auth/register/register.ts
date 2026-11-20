import { ThemeToggle } from '../../../shared/theme-toggle/theme-toggle';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { apiMessage } from '../../../core/services/toast.service';

/** Vérifie que password === confirm */
function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const p = group.get('password')?.value;
  const c = group.get('confirm')?.value;
  return p && c && p !== c ? { mismatch: true } : null;
}

@Component({
  selector: 'app-register',
  imports: [ThemeToggle, ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
})
export class Register {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly submitted = signal(false);

  form = this.fb.nonNullable.group(
    {
      prenom: ['', [Validators.required, Validators.maxLength(80)]],
      nom: ['', [Validators.required, Validators.maxLength(80)]],
      email: ['', [Validators.required, Validators.email]],
      tel: ['', [Validators.pattern(/^[+0-9 ]{6,20}$/)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirm: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  invalid(name: 'prenom' | 'nom' | 'email' | 'tel' | 'password' | 'confirm') {
    return this.submitted() && this.form.controls[name].invalid;
  }

  submit() {
    this.submitted.set(true);
    this.error.set(null);
    if (this.form.invalid) return;

    this.loading.set(true);
    const { confirm: _confirm, ...data } = this.form.getRawValue();
    this.auth.register(data).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.success.set(res.message);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(apiMessage(err, 'Inscription impossible.'));
      },
    });
  }
}
