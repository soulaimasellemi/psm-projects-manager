export type RoleName = 'ADMIN' | 'COLLABORATEUR';
export type StatutProjet = 'EN_ATTENTE' | 'EN_COURS' | 'TERMINE';

export interface User {
  id: number;
  email: string;
  tel?: string | null;
  nom: string;
  prenom: string;
  enabled: boolean;
  roles: RoleName[];
  dateCreation?: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface RegisterRequest {
  email: string;
  password: string;
  tel: string;
  nom: string;
  prenom: string;
}

export interface Client {
  id: number;
  nom: string;
  tel?: string | null;
  email?: string | null;
  adresse?: string | null;
  secteur?: string | null;
  nbProjets: number;
  budgetTotal: number;
}

export interface ClientRequest {
  nom: string;
  tel: string;
  email: string;
  adresse: string;
  secteur: string;
}

export interface ClientOption {
  id: number;
  nom: string;
}

export interface Projet {
  id: number;
  libelle: string;
  budget: number;
  duree: number;
  description?: string | null;
  statut: StatutProjet;
  idClient: number;
  nomClient: string;
}

export interface ProjetRequest {
  libelle: string;
  budget: number;
  duree: number;
  description: string;
  statut: StatutProjet;
  idClient: number;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Stats {
  nbClients: number;
  nbProjets: number;
  budgetTotal: number;
  nbUsers: number;
  usersEnAttente: number;
  projetsParStatut: Record<StatutProjet, number>;
  topClients: { nom: string; budget: number }[];
  derniersProjets: Projet[];
}

export const STATUTS: { value: StatutProjet; label: string; color: string }[] = [
  { value: 'EN_ATTENTE', label: 'En attente', color: 'warning' },
  { value: 'EN_COURS', label: 'En cours', color: 'info' },
  { value: 'TERMINE', label: 'Terminé', color: 'success' },
];

export function statutInfo(s: StatutProjet) {
  return STATUTS.find((x) => x.value === s) ?? STATUTS[0];
}

export const SECTEURS = [
  'Banque & Finance',
  'E-commerce',
  'Transport & Logistique',
  'Tourisme',
  'Santé',
  'Agroalimentaire',
  'Éducation',
  'Industrie',
  'Télécoms',
  'Autre',
];

/** Initiales pour les avatars : "Carthage Bank" -> "CB" */
export function initiales(texte: string | null | undefined): string {
  return (texte ?? '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((m) => m[0]!.toUpperCase())
    .join('');
}
