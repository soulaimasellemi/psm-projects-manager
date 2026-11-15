# Guide de déploiement sur un VPS (Partie 3)

## C'est quoi un VPS ?

Un **VPS** (*Virtual Private Server*) est un serveur Linux que vous louez en ligne, en général à partir de 4 à 6 € par mois chez OVH, Hostinger, Contabo ou DigitalOcean.
Il reste allumé 24 h/24 et possède une **adresse IP publique**. Une fois l'application installée dessus, n'importe qui peut l'ouvrir depuis son navigateur, par exemple à `http://51.75.12.34`.

Vous y accédez depuis Windows avec la commande `ssh`, déjà incluse dans PowerShell.
Ce guide suppose un VPS sous **Ubuntu 24.04** avec au moins **2 Go de RAM**.

> Pas de VPS ? Vous pouvez déjà valider la Partie 3 en local avec `java -jar psm.jar` (voir le README). Les étapes ci-dessous sont les mêmes quel que soit l'hébergeur.

---

## Étape 0 : Construire psm.jar sur votre PC

Utilisez la méthode A du README (STS), ou double-cliquez sur `build-jar.bat` (voir le README). Le résultat est `psm-backend\target\psm.jar`.

## Étape 1 : Se connecter au VPS

L'hébergeur vous envoie une **IP**, un utilisateur (`root` ou `ubuntu`) et un mot de passe. Dans PowerShell :

```powershell
ssh root@VOTRE_IP
```

## Étape 2 : Installer Java et MySQL

```bash
apt update && apt upgrade -y
apt install -y openjdk-21-jre-headless mysql-server
java -version          # doit afficher 21.x
systemctl status mysql # doit être "active (running)"
```

> Le projet est compilé en bytecode Java 21 : il tourne avec Java 21 ou une version plus récente.

## Étape 3 : Créer la base et un utilisateur MySQL dédié

```bash
mysql
```

```sql
CREATE DATABASE psm_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'psm'@'localhost' IDENTIFIED BY 'UnMotDePasseSolide!2026';
GRANT ALL PRIVILEGES ON psm_db.* TO 'psm'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

## Étape 4 : Envoyer psm.jar sur le serveur

Sur le VPS, créez le dossier :

```bash
mkdir -p /opt/psm
```

Puis, **depuis PowerShell sur votre PC** (dans le dossier du projet) :

```powershell
scp psm-backend\target\psm.jar root@VOTRE_IP:/opt/psm/psm.jar
```

## Étape 5 : Fichier de configuration (secrets)

Sur le VPS :

```bash
nano /opt/psm/psm.env
```

Collez ce contenu en l'adaptant (Ctrl+O pour enregistrer, Ctrl+X pour quitter) :

```bash
PORT=80
DB_URL=jdbc:mysql://localhost:3306/psm_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Africa/Tunis
DB_USERNAME=psm
DB_PASSWORD=UnMotDePasseSolide!2026
MAIL_USERNAME=votre.adresse@gmail.com
MAIL_PASSWORD=abcdefghijklmnop
ADMIN_NOTIFY_EMAIL=votre.adresse@gmail.com
FRONTEND_URL=http://VOTRE_IP
CORS_ORIGINS=http://VOTRE_IP
JWT_SECRET=COLLEZ_ICI_LE_RESULTAT_DE_LA_COMMANDE_CI-DESSOUS
INIT_ADMIN_EMAIL=votre.adresse@gmail.com
INIT_ADMIN_PASSWORD=UnAutreMotDePasseSolide!2026
DEMO_DATA=false
```

`INIT_ADMIN_EMAIL` / `INIT_ADMIN_PASSWORD` créent votre compte administrateur au premier démarrage (actif, rôle ADMIN). Changez ensuite ce mot de passe depuis la page **Mon profil**. `DEMO_DATA=false` évite de créer les comptes de démonstration (`admin@smartsoft.tn` / `Admin@123`), dont le mot de passe est public.

Générez une clé JWT propre au serveur, puis collez-la dans `JWT_SECRET` :

```bash
openssl rand -base64 64 | tr -d '\n'; echo
chmod 600 /opt/psm/psm.env
```

Ces variables **remplacent** les valeurs par défaut de `application.properties`, grâce à la syntaxe `${VARIABLE:defaut}`.

## Étape 6 : Lancer l'application comme service

Un service redémarre automatiquement l'application après une panne ou un redémarrage du serveur.

```bash
nano /etc/systemd/system/psm.service
```

```ini
[Unit]
Description=Projects Manager (PSM) - SmartSoft
After=network.target mysql.service

[Service]
WorkingDirectory=/opt/psm
EnvironmentFile=/opt/psm/psm.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/psm/psm.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable --now psm
systemctl status psm          # doit afficher "active (running)"
journalctl -u psm -f          # logs en direct (Ctrl+C pour quitter)
```

## Étape 7 : Ouvrir le pare-feu

```bash
ufw allow OpenSSH
ufw allow 80/tcp
ufw enable
```

Ouvrez **http://VOTRE_IP** dans votre navigateur : la page de connexion PSM s'affiche. 🎉
Pour tester l'API avec Postman, remplacez la variable `baseUrl` par `http://VOTRE_IP/api`.

---

## Mettre à jour l'application

1. Sur votre PC, relancez `build-jar.bat`.
2. Envoyez le nouveau fichier : `scp psm-backend\target\psm.jar root@VOTRE_IP:/opt/psm/psm.jar`
3. Sur le VPS : `systemctl restart psm`

## Aller plus loin (facultatif)

* **Nom de domaine + HTTPS** : achetez un domaine, pointez-le vers l'IP, installez **Nginx** comme reverse proxy (l'application sur le port 8080, Nginx sur 80/443), puis obtenez un certificat gratuit avec `certbot --nginx`.
* **Sauvegarde de la base** : `mysqldump -u psm -p psm_db > sauvegarde.sql`

## Dépannage

| Problème | Commande / solution |
|---|---|
| Le service ne démarre pas | `journalctl -u psm -n 100`, puis lire l'erreur. |
| `Access denied for user 'psm'` | Vérifiez `DB_PASSWORD` dans `psm.env`, puis `systemctl restart psm`. |
| Page inaccessible | `ufw status` (le port 80 doit être autorisé) et `systemctl status psm`. |
| Port 80 refusé | Mettez `PORT=8080` dans `psm.env` et ouvrez `ufw allow 8080/tcp`. L'URL devient `http://IP:8080`. |
| Manque de mémoire | Réduisez `-Xmx512m` à `-Xmx384m`, ou prenez un VPS avec plus de RAM. |
