import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, RegisterRequest, RoleName, User } from '../models';

const TOKEN_KEY = 'psm_token';
const USER_KEY = 'psm_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private readonly api = `${environment.apiUrl}/auth`;

  private readonly _user = signal<User | null>(this.readUser());
  /** Utilisateur connecté (signal : l'interface se met à jour automatiquement) */
  readonly user = this._user.asReadonly();
  readonly isAdmin = computed(() => this._user()?.roles.includes('ADMIN') ?? false);
  readonly displayName = computed(() => {
    const u = this._user();
    return u ? `${u.prenom} ${u.nom}` : '';
  });

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  login(email: string, password: string) {
    return this.http
      .post<AuthResponse>(`${this.api}/login`, { email, password })
      .pipe(tap((res) => this.store(res)));
  }

  register(data: RegisterRequest) {
    return this.http.post<{ message: string }>(`${this.api}/register`, data);
  }

  /** Mot de passe oublié, étape 1 : demande du lien par email */
  forgotPassword(email: string) {
    return this.http.post<{ message: string }>(`${this.api}/forgot-password`, { email });
  }

  /** Mot de passe oublié, étape 2 : nouveau mot de passe avec le jeton du lien */
  resetPassword(token: string, password: string) {
    return this.http.post<{ message: string }>(`${this.api}/reset-password`, { token, password });
  }

  logout(redirect = true) {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this._user.set(null);
    if (redirect) this.router.navigate(['/login']);
  }

  /** Connecté = token présent, non expiré, et utilisateur en mémoire */
  isLoggedIn(): boolean {
    const token = this.token;
    return !!token && !this.isExpired(token) && !!this._user();
  }

  hasRole(...roles: RoleName[]): boolean {
    const u = this._user();
    return !!u && roles.some((r) => u.roles.includes(r));
  }

  /** Met à jour l'utilisateur stocké (après modification du profil) */
  updateUser(user: User) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this._user.set(user);
  }

  private store(res: AuthResponse) {
    localStorage.setItem(TOKEN_KEY, res.token);
    this.updateUser(res.user);
  }

  private readUser(): User | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? (JSON.parse(raw) as User) : null;
    } catch {
      return null;
    }
  }

  /** Lit la date d'expiration (exp) contenue dans le JWT */
  private isExpired(token: string): boolean {
    try {
      const part = token.split('.')[1]!.replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(part)) as { exp?: number };
      return !payload.exp || payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
}
