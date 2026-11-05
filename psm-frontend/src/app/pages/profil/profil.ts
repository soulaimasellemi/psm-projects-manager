import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { UserService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { User, initiales } from '../../core/models';

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const n = group.get('nouveau')?.value;
  const c = group.get('confirm')?.value;
  return n && c && n !== c ? { mismatch: true } : null;
}

@Component({
  selector: 'app-profil',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './profil.html',
})
export class Profil implements OnInit {
  private fb = inject(FormBuilder);
  private service = inject(UserService);
  private toast = inject(ToastService);
  auth = inject(AuthService);

  readonly me = signal<User | null>(this.auth.user());
  readonly savingInfo = signal(false);
  readonly savingPwd = signal(false);
  readonly pwdSubmitted = signal(false);

  infoForm = this.fb.nonNullable.group({
    prenom: ['', [Validators.required, Validators.maxLength(80)]],
    nom: ['', [Validators.required, Validators.maxLength(80)]],
    tel: ['', [Validators.maxLength(30)]],
  });

  pwdForm = this.fb.nonNullable.group(
    {
      ancien: ['', Validators.required],
      nouveau: ['', [Validators.required, Validators.minLength(6)]],
      confirm: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  ngOnInit() {
    this.fill(this.auth.user());
    this.service.me().subscribe({
      next: (u) => {
        this.me.set(u);
        this.auth.updateUser(u);
        this.fill(u);
      },
    });
  }

  initiales() {
    const u = this.me();
    return initiales(u ? `${u.prenom} ${u.nom}` : '');
  }

  saveInfo() {
    if (this.infoForm.invalid) return;
    this.savingInfo.set(true);
    this.service.updateMe(this.infoForm.getRawValue()).subscribe({
      next: (u) => {
        this.savingInfo.set(false);
        this.me.set(u);
        this.auth.updateUser(u);
        this.toast.success('Profil mis à jour.');
      },
      error: (e) => {
        this.savingInfo.set(false);
        this.toast.apiError(e);
      },
    });
  }

  savePassword() {
    this.pwdSubmitted.set(true);
    if (this.pwdForm.invalid) return;
    this.savingPwd.set(true);
    const { ancien, nouveau } = this.pwdForm.getRawValue();
    this.service.changePassword(ancien, nouveau).subscribe({
      next: (res) => {
        this.savingPwd.set(false);
        this.pwdForm.reset();
        this.pwdSubmitted.set(false);
        this.toast.success(res.message);
      },
      error: (e) => {
        this.savingPwd.set(false);
        this.toast.apiError(e);
      },
    });
  }

  private fill(u: User | null) {
    if (u) this.infoForm.reset({ prenom: u.prenom, nom: u.nom, tel: u.tel ?? '' });
  }
}
