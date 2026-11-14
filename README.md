# Projects Manager (PSM) — SmartSoft Solutions

Application full-stack **Spring Boot 3.5 + Angular 21** pour gérer les clients et les projets d'une agence digitale.
Elle suit le cahier des charges : entités User / Role / Client / Projet, Spring Security + JWT, email à l'admin après une inscription, Guards, Interceptor, rôles Admin / Collaborateur et template Argon Dashboard.

```
psm/
├── psm-backend/      → API Spring Boot (Maven)
├── psm-frontend/     → Application Angular 21 (thème Argon personnalisé)
├── postman/          → Collection Postman prête à importer
├── docs/DEPLOIEMENT.md → Guide de déploiement sur un VPS
└── build-jar.bat     → Construit psm.jar (Angular inclus) en un double-clic
```

---

## 1. Prérequis (déjà installés chez vous)

| Outil | Version | Vérifier |
|---|---|---|
| Java (JDK) | 21 ou + (vous avez 25) | `java -version` |
| Node.js / npm | 24 / 11 | `node -v` |
| Angular CLI | 21 | `ng version` |
| MySQL Server + Workbench | 8.x | MySQL doit être démarré |
| Maven | 3.9 (seulement pour construire psm.jar) | `mvn -v` |
| Postman | — | — |

### Vos outils

| Partie | Outil |
|---|---|
| Backend `psm-backend` | **Spring Tool Suite (STS)** |
| Frontend `psm-frontend` | **VS Code** (+ extension *Angular Language Service*) |

> STS contient déjà Maven : vous n'avez rien à installer pour lancer le backend, ni pour construire `psm.jar`.

---

## 2. Configuration du backend

Ouvrez `psm-backend/src/main/resources/application.properties` et modifiez les lignes marquées **A MODIFIER** :

```properties
spring.datasource.password=${DB_PASSWORD:VOTRE_MOT_DE_PASSE_MYSQL}
spring.mail.username=${MAIL_USERNAME:votre.adresse@gmail.com}
spring.mail.password=${MAIL_PASSWORD:abcdefghijklmnop}
psm.admin.email=${ADMIN_NOTIFY_EMAIL:votre.adresse@gmail.com}
```

* **MySQL** : mettez le mot de passe `root` que vous utilisez dans Workbench. La base `psm_db` et les tables sont **créées automatiquement**.
* **Gmail** : il faut un *mot de passe d'application*, pas votre vrai mot de passe :
  1. Activez la **validation en 2 étapes** sur votre compte Google (myaccount.google.com → Sécurité).
  2. Allez sur **myaccount.google.com/apppasswords**, nommez l'application `PSM`, puis cliquez sur *Créer*.
  3. Copiez les 16 lettres **sans les espaces** dans `spring.mail.password`.
* `psm.admin.email` est l'adresse qui **reçoit** la notification à chaque inscription. Mettez la vôtre pour la démo.

> ⚠️ Ne publiez jamais ces mots de passe sur GitHub. Sur le VPS, on les passera en variables d'environnement.

---

## 3. Lancer le backend

**Importer le projet dans STS (une seule fois)** :
1. *File → Import… → Maven → Existing Maven Projects*, puis *Next*.
2. *Root Directory* : choisissez le dossier `psm-backend`, cochez `pom.xml`, puis *Finish*.
3. Patientez pendant le téléchargement des dépendances (barre de progression en bas à droite).
   Si des erreurs rouges restent affichées : clic droit sur le projet → *Maven → Update Project…* (Alt+F5), cochez *Force Update*, puis *OK*.

**Lancer** : clic droit sur le projet `psm` → *Run As → Spring Boot App*, ou utilisez le **Boot Dashboard** (en bas à gauche) puis ▶.
Les logs s'affichent dans l'onglet *Console*.

> Astuce : au lieu d'écrire vos mots de passe dans `application.properties`, vous pouvez les mettre dans *Run → Run Configurations… → Spring Boot App → psm → onglet Environment*, avec les variables `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD` et `ADMIN_NOTIFY_EMAIL`.

L'API démarre sur **http://localhost:8080**. Au premier démarrage, ces données sont créées :

| Compte | Email | Mot de passe | Rôle |
|---|---|---|---|
| Admin | `admin@smartsoft.tn` | `Admin@123` | ADMIN |
| Collaborateur | `collab@smartsoft.tn` | `Collab@123` | COLLABORATEUR |
| En attente | `yassine.gharbi@smartsoft.tn` | `Yassine@123` | COLLABORATEUR (inactif) |

S'y ajoutent 6 clients et 10 projets de démonstration (banque, e-commerce, logistique, tourisme, santé, agroalimentaire).

---

## 4. Lancer le frontend

Dans VS Code : *Fichier → Ouvrir le dossier…*, puis choisissez `psm-frontend`. Ouvrez ensuite un terminal (*Terminal → Nouveau terminal*) :

```bash
npm install
ng serve
```

Vous pouvez aussi lancer **Angular - ng serve + Chrome** depuis *Exécuter et déboguer* : la commande démarre `ng serve`, puis ouvre Chrome avec le débogage actif.

Ouvrez **http://localhost:4200**. Sur la page de connexion, les boutons « Admin » et « Collaborateur » remplissent le formulaire automatiquement.

---

## 5. Tester avec Postman

1. Dans Postman, cliquez sur **Import**, puis choisissez `postman/PSM.postman_collection.json`.
2. Lancez **1. Auth → Login ADMIN**, puis **Login COLLABORATEUR**. Les tokens sont enregistrés automatiquement dans les variables `token` et `tokenCollab`.
3. Exécutez les dossiers dans l'ordre. Le *Collection Runner* peut tout lancer d'un coup.
4. Le dossier **5. Tests de sécurité** vérifie les droits :
   * sans token → 401 ;
   * un collaborateur qui modifie ou supprime → 403 ;
   * un collaborateur qui ajoute → 201.

### Endpoints de l'API

| Méthode | URL | Accès |
|---|---|---|
| POST | `/api/auth/register` | public (compte inactif + email à l'admin) |
| POST | `/api/auth/login` | public → `{ token, user }` |
| GET | `/api/clients?q=&page=&size=` · `/api/clients/{id}` · `/api/clients/all` | Admin, Collaborateur |
| POST | `/api/clients` | Admin, Collaborateur |
| PUT / DELETE | `/api/clients/{id}` | **Admin** |
| POST | `/api/clients/{id}/fichier` (multipart, champ `file`) | Admin, Collaborateur — upload PDF / image / Word, 5 Mo max |
| GET | `/api/clients/{id}/fichier` | Admin, Collaborateur — téléchargement |
| DELETE | `/api/clients/{id}/fichier` | **Admin** |
| GET | `/api/projets?q=&statut=&clientId=&page=&size=` · `/api/projets/{id}` | Admin, Collaborateur |
| POST | `/api/projets` | Admin, Collaborateur |
| PUT / DELETE | `/api/projets/{id}` | **Admin** |
| GET / PUT | `/api/users/me` · PUT `/api/users/me/password` | tout utilisateur connecté |
| GET | `/api/users?q=&enabled=` | **Admin** |
| PUT | `/api/users/{id}/statut` · `/api/users/{id}/roles` | **Admin** |
| DELETE | `/api/users/{id}` | **Admin** |
| GET | `/api/stats` | Admin, Collaborateur |

---

## 6. Architecture

### Backend (`com.smartsoft.psm`)

| Package | Rôle |
|---|---|
| `entity` | `User` (implémente `UserDetails`), `Role`, `Client`, `Projet`, `StatutProjet`. User ↔ Role en **ManyToMany** (`user_roles`), Client → Projet en **OneToMany** (`id_client`). |
| `repository` | Spring Data JPA, avec des requêtes JPQL pour la recherche, les filtres et les statistiques. |
| `dto` | Records Java avec validation (`@NotBlank`, `@Email`…) ; les entités ne sont jamais exposées directement. |
| `service` | Logique métier. `EmailService` envoie en asynchrone (`@Async`). |
| `controller` | Contrôleurs REST. |
| `security` | `SecurityConfig` (règles par rôle et méthode HTTP), `JwtService`, `JwtAuthFilter`, BCrypt. |
| `exception` | `GlobalExceptionHandler` : renvoie des erreurs JSON claires (400, 401, 403, 404, 409). |
| `config` | `DataInitializer` (données de démo), `SpaWebConfig` (sert Angular dans le .jar). |

### Frontend (`src/app`)

| Dossier | Rôle |
|---|---|
| `core/services` | `AuthService` (signals, localStorage, expiration du JWT), services HTTP. |
| `core/guards` | `authGuard` (connecté ?), `roleGuard` (`data.roles`), `guestGuard`. |
| `core/interceptors` | `authInterceptor` : ajoute `Authorization: Bearer`, gère 401 (déconnexion) et 403. |
| `shared` | Directive `*appHasRole`, pagination, notifications (toasts). |
| `layout` | Sidebar, navbar et bandeau dégradé (style Argon). |
| `pages` | Login, Register, Dashboard (Chart.js), Clients, Projets, Utilisateurs, Profil. |

### Rôles dans l'interface

| | Admin | Collaborateur |
|---|---|---|
| Consulter et ajouter des clients / projets | ✅ | ✅ |
| Modifier et supprimer | ✅ | ❌ (boutons masqués, et 403 côté API) |
| Page Utilisateurs (valider les comptes, gérer les rôles) | ✅ | ❌ (bloquée par le `roleGuard`) |

### Fonctionnalités en plus du cahier des charges

* **Dashboard** : 4 indicateurs, graphique des 5 meilleurs clients par budget, répartition des projets par statut, derniers projets.
* **Recherche, filtres et pagination** sur les clients, les projets et les utilisateurs.
* **Email à l'admin à chaque nouveau client** (en plus de l'email d'inscription).
* **Fichier joint par client** (upload / download), stocké dans le dossier `uploads/` (variable `UPLOAD_DIR`).
* **Validation des inscriptions par l'admin** : un email lui est envoyé à l'inscription, puis un autre est envoyé à l'utilisateur quand son compte est activé.
* **Page profil** : modifier ses informations et son mot de passe.

---

## 7. Construire psm.jar (Partie 3)

**Méthode A, avec STS (recommandée, sans installer Maven)** :
1. Dans un terminal VS Code, depuis `psm-frontend` : `ng build`. Le résultat est dans `dist/psm-frontend/browser/`.
2. Copiez **le contenu** du dossier `dist/psm-frontend/browser/` dans `psm-backend/src/main/resources/static/`.
3. Dans STS : clic droit sur le projet → *Refresh* (F5).
4. Clic droit sur le projet → *Run As → Maven build…*. Dans *Goals*, tapez `clean package`, cochez *Skip Tests*, puis cliquez sur *Run*.
5. Le fichier **`psm-backend/target/psm.jar`** est créé. Faites *Refresh* pour voir le dossier `target`.

**Méthode B, script automatique** : double-cliquez sur **`build-jar.bat`** à la racine du projet. Ce script fait les mêmes étapes, mais il exige que Maven soit installé dans le PATH :
1. téléchargez `apache-maven-3.9.x-bin.zip` sur maven.apache.org ;
2. décompressez-le dans `C:\maven` ;
3. ajoutez `C:\maven\bin` au *Path* de Windows.

Pour tester : `java -jar psm-backend/target/psm.jar`, puis ouvrez **http://localhost:8080**. Angular et l'API sont alors servis ensemble par Spring Boot.

Le déploiement sur un serveur est expliqué pas à pas dans **[docs/DEPLOIEMENT.md](docs/DEPLOIEMENT.md)**.

---

## 8. Problèmes fréquents

| Symptôme | Solution |
|---|---|
| `Access denied for user 'root'` | Mauvais mot de passe MySQL dans `application.properties`. |
| `Communications link failure` | MySQL n'est pas démarré (Services Windows → MySQL80 → Démarrer). |
| Toast « Serveur injoignable » dans Angular | Le backend n'est pas lancé sur le port 8080. |
| Erreur CORS dans la console | Angular doit tourner sur `http://localhost:4200` (voir `psm.cors.allowed-origins`). |
| L'email n'arrive pas | Vérifiez le mot de passe d'application Gmail. Le log affiche `Échec d'envoi de l'email…`. L'inscription fonctionne quand même. |
| Erreur 403 au login | Le compte n'est pas encore validé : connectez-vous en admin, puis *Utilisateurs → Valider*. |
| `npm install` échoue (peer deps) | Lancez `npm install --legacy-peer-deps`. |
