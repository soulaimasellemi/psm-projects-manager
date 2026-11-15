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

/** Création directe d'un compte par l'ADMIN (POST /api/users) */
export interface CreateUserRequest extends RegisterRequest {
  roles: RoleName[];
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
  dateDebut?: string | null;
  dateFinPrevue?: string | null;
  avancement: number;
  enRetard: boolean;
  membres: Membre[];
  nbTaches: number;
  nbTachesTerminees: number;
  sante: Sante;
}

// ---------- Santé d'un projet (détection des risques) ----------
export type NiveauSante = 'SAIN' | 'A_SURVEILLER' | 'CRITIQUE';

export interface Sante {
  score: number;
  niveau: NiveauSante;
  raisons: string[];
}

export const NIVEAUX_SANTE: Record<NiveauSante, { label: string; color: string; icon: string }> = {
  SAIN: { label: 'Sain', color: 'success', icon: 'fa-heart-pulse' },
  A_SURVEILLER: { label: 'À surveiller', color: 'warning', icon: 'fa-triangle-exclamation' },
  CRITIQUE: { label: 'Critique', color: 'danger', icon: 'fa-fire' },
};

// ---------- Tâches (Kanban) ----------
export type StatutTache = 'A_FAIRE' | 'EN_COURS' | 'TERMINEE';
export type PrioriteTache = 'BASSE' | 'MOYENNE' | 'HAUTE';

export interface Tache {
  id: number;
  titre: string;
  description?: string | null;
  statut: StatutTache;
  priorite: PrioriteTache;
  echeance?: string | null;
  ordre: number;
  enRetard: boolean;
  projetId: number;
  assigne?: Membre | null;
  dateCreation?: string;
  dateTerminee?: string | null;
}

export interface TacheRequest {
  titre: string;
  description: string;
  statut: StatutTache;
  priorite: PrioriteTache;
  echeance: string | null;
  assigneId: number | null;
}

export const COLONNES_KANBAN: { value: StatutTache; label: string; color: string; icon: string }[] = [
  { value: 'A_FAIRE', label: 'À faire', color: 'secondary', icon: 'fa-list-check' },
  { value: 'EN_COURS', label: 'En cours', color: 'info', icon: 'fa-spinner' },
  { value: 'TERMINEE', label: 'Terminée', color: 'success', icon: 'fa-circle-check' },
];

export const PRIORITES: Record<PrioriteTache, { label: string; color: string }> = {
  HAUTE: { label: 'Haute', color: 'danger' },
  MOYENNE: { label: 'Moyenne', color: 'warning' },
  BASSE: { label: 'Basse', color: 'info' },
};

/** Collaborateur affecté à un projet (et liste des comptes actifs : GET /api/users/options) */
export interface Membre {
  id: number;
  nomComplet: string;
  email: string;
}

export interface ProjetRequest {
  libelle: string;
  budget: number;
  duree: number;
  description: string;
  statut: StatutProjet;
  idClient: number;
  dateDebut: string | null;
  dateFinPrevue: string | null;
  avancement: number;
  membresIds: number[];
}

/** Filtres de la liste des projets (et de l'export) */
export interface ProjetFiltres {
  q: string;
  statut: StatutProjet | '';
  clientId: number | null;
  membreId: number | null;
  enRetard: boolean;
}

export type ExportFormat = 'xlsx' | 'pdf';

export type ActionActivite =
  | 'CREATION' | 'MODIFICATION' | 'SUPPRESSION' | 'CONNEXION' | 'INSCRIPTION' | 'VALIDATION' | 'MOT_DE_PASSE';
export type CibleActivite = 'CLIENT' | 'PROJET' | 'TACHE' | 'UTILISATEUR';

/** Ligne de l'historique des actions (GET /api/activites) */
export interface Activite {
  id: number;
  dateAction: string;
  auteurEmail?: string | null;
  auteurNom?: string | null;
  action: ActionActivite;
  cible: CibleActivite;
  cibleId?: number | null;
  details?: string | null;
}

export const ACTIONS: Record<ActionActivite, { label: string; color: string; icon: string }> = {
  CREATION: { label: 'Création', color: 'success', icon: 'fa-plus' },
  MODIFICATION: { label: 'Modification', color: 'info', icon: 'fa-pen' },
  SUPPRESSION: { label: 'Suppression', color: 'danger', icon: 'fa-trash' },
  CONNEXION: { label: 'Connexion', color: 'primary', icon: 'fa-right-to-bracket' },
  INSCRIPTION: { label: 'Inscription', color: 'warning', icon: 'fa-user-plus' },
  VALIDATION: { label: 'Validation', color: 'success', icon: 'fa-user-check' },
  MOT_DE_PASSE: { label: 'Mot de passe', color: 'warning', icon: 'fa-key' },
};

export const CIBLES: Record<CibleActivite, { label: string; icon: string }> = {
  CLIENT: { label: 'Client', icon: 'fa-building' },
  PROJET: { label: 'Projet', icon: 'fa-diagram-project' },
  TACHE: { label: 'Tâche', icon: 'fa-list-check' },
  UTILISATEUR: { label: 'Utilisateur', icon: 'fa-user' },
};

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
  projetsEnRetard: number;
  avancementMoyen: number;
  avancements: { id: number; libelle: string; avancement: number; enRetard: boolean }[];
  mesProjets: number;
  santeMoyenne: number;
  nbProjetsCritiques: number;
  nbProjetsASurveiller: number;
  projetsARisque: { id: number; libelle: string; nomClient: string; score: number; niveau: NiveauSante; raisons: string[] }[];
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
