import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Activite, ActionActivite, CibleActivite, Client, ClientOption, ClientRequest, CreateUserRequest, ExportFormat,
  Membre, Page, Projet, ProjetFiltres, ProjetRequest, RoleName, StatutTache, Stats, Tache, TacheRequest, User,
} from '../models';

/** Construit des HttpParams en ignorant les valeurs vides */
function params(values: Record<string, string | number | boolean | null | undefined>): HttpParams {
  let p = new HttpParams();
  for (const [k, v] of Object.entries(values)) {
    if (v !== null && v !== undefined && v !== '') p = p.set(k, String(v));
  }
  return p;
}

/**
 * Enregistre un fichier renvoyé par l'API (export Excel / PDF).
 * Le nom vient de l'en-tête Content-Disposition (exposé par la config CORS du backend).
 */
function telecharger(obs: Observable<HttpResponse<Blob>>, nomParDefaut: string) {
  return obs.pipe(
    tap((res) => {
      const dispo = res.headers.get('Content-Disposition') ?? '';
      const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(dispo);
      const simple = /filename="?([^";]+)"?/i.exec(dispo);
      const nom = utf8 ? decodeURIComponent(utf8[1]!) : simple ? simple[1]! : nomParDefaut;
      const url = URL.createObjectURL(res.body!);
      const a = document.createElement('a');
      a.href = url;
      a.download = nom;
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    }),
  );
}

@Injectable({ providedIn: 'root' })
export class ClientService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/clients`;

  list(q: string, page: number, size: number) {
    return this.http.get<Page<Client>>(this.api, { params: params({ q, page, size }) });
  }
  options() { return this.http.get<ClientOption[]>(`${this.api}/all`); }
  get(id: number) { return this.http.get<Client>(`${this.api}/${id}`); }
  create(data: ClientRequest) { return this.http.post<Client>(this.api, data); }
  update(id: number, data: ClientRequest) { return this.http.put<Client>(`${this.api}/${id}`, data); }
  delete(id: number) { return this.http.delete<void>(`${this.api}/${id}`); }

  /** Télécharge la liste (filtrée par la recherche) en Excel ou PDF */
  export(format: ExportFormat, q: string) {
    return telecharger(
      this.http.get(`${this.api}/export`, { params: params({ format, q }), responseType: 'blob', observe: 'response' }),
      `psm-clients.${format}`,
    );
  }
}

/** Filtres projets -> paramètres d'URL (enRetard n'est envoyé que s'il est coché) */
function filtres(f: ProjetFiltres) {
  return { q: f.q, statut: f.statut, clientId: f.clientId, membreId: f.membreId, enRetard: f.enRetard || null };
}

@Injectable({ providedIn: 'root' })
export class ProjetService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/projets`;

  list(f: ProjetFiltres, page: number, size: number) {
    return this.http.get<Page<Projet>>(this.api, { params: params({ ...filtres(f), page, size }) });
  }
  get(id: number) { return this.http.get<Projet>(`${this.api}/${id}`); }
  create(data: ProjetRequest) { return this.http.post<Projet>(this.api, data); }
  update(id: number, data: ProjetRequest) { return this.http.put<Projet>(`${this.api}/${id}`, data); }
  delete(id: number) { return this.http.delete<void>(`${this.api}/${id}`); }

  /** Télécharge la liste (mêmes filtres que l'écran) en Excel ou PDF */
  export(format: ExportFormat, f: ProjetFiltres) {
    return telecharger(
      this.http.get(`${this.api}/export`, { params: params({ format, ...filtres(f) }), responseType: 'blob', observe: 'response' }),
      `psm-projets.${format}`,
    );
  }
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/users`;

  /** Comptes actifs à affecter aux projets (ADMIN + COLLABORATEUR) */
  options() { return this.http.get<Membre[]>(`${this.api}/options`); }

  // Administration (ADMIN)
  list(q: string, enabled: boolean | null, page: number, size: number) {
    return this.http.get<Page<User>>(this.api, { params: params({ q, enabled, page, size }) });
  }
  create(data: CreateUserRequest) { return this.http.post<User>(this.api, data); }
  setStatut(id: number, enabled: boolean) { return this.http.put<User>(`${this.api}/${id}/statut`, { enabled }); }
  setRoles(id: number, roles: RoleName[]) { return this.http.put<User>(`${this.api}/${id}/roles`, { roles }); }
  delete(id: number) { return this.http.delete<void>(`${this.api}/${id}`); }

  // Profil (tout utilisateur connecté)
  me() { return this.http.get<User>(`${this.api}/me`); }
  updateMe(data: { nom: string; prenom: string; tel: string }) { return this.http.put<User>(`${this.api}/me`, data); }
  changePassword(ancien: string, nouveau: string) {
    return this.http.put<{ message: string }>(`${this.api}/me/password`, { ancien, nouveau });
  }
}

@Injectable({ providedIn: 'root' })
export class StatsService {
  private http = inject(HttpClient);
  get() { return this.http.get<Stats>(`${environment.apiUrl}/stats`); }
}

/** Tâches du Kanban d'un projet */
@Injectable({ providedIn: 'root' })
export class TacheService {
  private http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  list(projetId: number) { return this.http.get<Tache[]>(`${this.api}/projets/${projetId}/taches`); }
  create(projetId: number, data: TacheRequest) {
    return this.http.post<Tache>(`${this.api}/projets/${projetId}/taches`, data);
  }
  update(id: number, data: TacheRequest) { return this.http.put<Tache>(`${this.api}/taches/${id}`, data); }
  /** Glisser-déposer : nouvelle colonne + nouvelle position */
  deplacer(id: number, statut: StatutTache, ordre: number) {
    return this.http.put<Tache>(`${this.api}/taches/${id}/deplacer`, { statut, ordre });
  }
  delete(id: number) { return this.http.delete<void>(`${this.api}/taches/${id}`); }
}

/** Historique des actions (ADMIN) */
@Injectable({ providedIn: 'root' })
export class ActiviteService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/activites`;

  list(q: string, cible: CibleActivite | '', action: ActionActivite | '', page: number, size: number) {
    return this.http.get<Page<Activite>>(this.api, { params: params({ q, cible, action, page, size }) });
  }
}
