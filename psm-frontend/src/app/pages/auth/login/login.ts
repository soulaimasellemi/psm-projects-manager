import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService, apiMessage } from '../../../core/services/toast.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
})
export class Login {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly submitted = signal(false);
  readonly showPassword = signal(false);

  form = this.fb.nonNullable.group({
    email: ['', Validators.required], // étape 1 : 'username' (ex. amine)
    password: ['', Validators.required],
  });

  /** Remplit le formulaire avec le compte du workshop */
  demo(_role: 'admin' | 'collab') {
    this.form.setValue({ email: 'amine', password: '1234' });
  }

  /** ÉTAPE 1 : checkLogin() du workshop (authentification en dur) */
  submit() {
    this.submitted.set(true);
    this.error.set(null);
    if (this.form.invalid) return;

    const { email, password } = this.form.getRawValue();
    if (this.auth.authenticate(email, password)) {
      this.router.navigate(['/dashboard']);
    } else {
      this.error.set('Invalid username or password');
    }
  }
}
