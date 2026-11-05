import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  Client, ClientOption, ClientRequest, Page, Projet, ProjetRequest, RoleName, Stats, StatutProjet, User,
} from '../models';

/** Construit des HttpParams en ignorant les valeurs vides */
function params(values: Record<string, string | number | boolean | null | undefined>): HttpParams {
  let p = new HttpParams();
  for (const [k, v] of Object.entries(values)) {
    if (v !== null && v !== undefined && v !== '') p = p.set(k, String(v));
  }
  return p;
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
}

@Injectable({ providedIn: 'root' })
export class ProjetService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/projets`;

  list(q: string, statut: StatutProjet | '', clientId: number | null, page: number, size: number) {
    return this.http.get<Page<Projet>>(this.api, { params: params({ q, statut, clientId, page, size }) });
  }
  create(data: ProjetRequest) { return this.http.post<Projet>(this.api, data); }
  update(id: number, data: ProjetRequest) { return this.http.put<Projet>(`${this.api}/${id}`, data); }
  delete(id: number) { return this.http.delete<void>(`${this.api}/${id}`); }
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/users`;

  // Administration (ADMIN)
  list(q: string, enabled: boolean | null, page: number, size: number) {
    return this.http.get<Page<User>>(this.api, { params: params({ q, enabled, page, size }) });
  }
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
