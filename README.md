# Budget App

Application Spring Boot de gestion de budget personnel et d'épargne, avec interface web Thymeleaf.

## Fonctionnalités

- **Budget** : saisie du solde courant, projection quotidienne jusqu'en fin de mois avec graphique d'évolution
- **Dépenses récurrentes** : gestion des dépenses mensuelles (catégorie, libellé, montant, jour du mois)
- **Épargne** : suivi de plusieurs comptes épargne avec simulation de progression et graphiques
- **Objectifs** : définition d'objectifs de solde cible ou de versement mensuel par compte épargne :
  - Histogramme comparant versements actuels vs objectifs de versement mensuel
  - Histogramme synthèse solde actuel vs solde objectif + courbes de tendance par compte
  - Calcul automatique de la date d'atteinte de l'objectif selon la tendance observée sur X années (paramétrable)
  - Alerte automatique si un objectif de solde est atteint mais que des versements sont encore actifs
  - Accès lecture VIEWER/EDITOR/ADMIN, modification réservée EDITOR et ADMIN
- **Administration** (réservée ADMIN) :
  - Gestion des catégories avec icônes emoji
  - Gestion des utilisateurs (3 rôles : ADMIN, EDITOR, VIEWER)
  - Clés API avec nom, durée de validité et historique d'utilisation
  - Import / Export JSON de toute la base de données
- **Sécurité** : login/mot de passe, passkeys (WebAuthn / FIDO2), remember-me 12 mois, protection CSRF
- **API REST** : endpoint `/api/export` protégé par clé API

## Prérequis

- Java 17+
- Maven 3.9+
- Docker (pour la production)

## Démarrage en local

```bash
# Variables d'environnement (ou valeurs par défaut admin/admin)
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=MonMotDePasse!

# Lancer l'application (profil local avec SQLite)
./mvnw spring-boot:run
```

L'application est accessible sur `http://localhost:8080`.

Un compte administrateur est créé automatiquement au premier démarrage avec les credentials définis dans les variables d'environnement.

## Profils

| Profil | Base de données | Usage |
|--------|----------------|-------|
| `local` (défaut) | SQLite (`./budget.db`) | Développement |
| `prod` | PostgreSQL | Production (Docker) |

## Déploiement Docker (Raspberry Pi ARM64)

### Variables d'environnement requises

| Variable | Description | Défaut |
|----------|-------------|--------|
| `ADMIN_USERNAME` | Login admin | `admin` |
| `ADMIN_PASSWORD` | Mot de passe admin | `admin` |
| `REMEMBER_ME_KEY` | Clé secrète remember-me | `budget-remember-me-secret-key` |
| `SPRING_DATASOURCE_URL` | URL PostgreSQL | — |
| `SPRING_DATASOURCE_USERNAME` | User PostgreSQL | — |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe PostgreSQL | — |
| `WEBAUTHN_RP_ID` | Domaine WebAuthn (sans port) | `localhost` |
| `WEBAUTHN_ALLOWED_ORIGINS` | Origines autorisées (virgule-séparées) | `http://localhost:8080` |
| `WEBAUTHN_RP_NAME` | Nom affiché dans le prompt passkey | `Budget App` |

> ⚠️ **WebAuthn requiert HTTPS en production.** Sur le Raspberry Pi, placez un reverse proxy TLS devant l'application (Traefik ou Nginx + Let's Encrypt) et configurez `WEBAUTHN_RP_ID` avec votre domaine réel.

### Exemple docker-compose.yml

```yaml
version: "3.8"
services:
  app:
    image: <votre-dockerhub>/budget-app:latest
    ports:
      - "8080:8080"
    environment:
      ADMIN_USERNAME: admin
      ADMIN_PASSWORD: ${ADMIN_PASSWORD}
      REMEMBER_ME_KEY: ${REMEMBER_ME_KEY}
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/budget
      SPRING_DATASOURCE_USERNAME: budget
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
    depends_on:
      - db
  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: budget
      POSTGRES_USER: budget
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
volumes:
  pgdata:
```

## CI/CD (GitHub Actions)

Le workflow `.github/workflows/docker-build.yml` :
1. Compile et teste avec Maven
2. Construit une image multi-plateforme (`amd64` + `arm64`)
3. Pousse sur Docker Hub (sur push vers `main` ou tag `v*`)

**Secrets GitHub requis :**
- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

## Authentification

### Login / Mot de passe

Formulaire classique accessible sur `/login`. Le compte administrateur est créé automatiquement au premier démarrage.

### Passkeys (WebAuthn / FIDO2)

Les passkeys permettent une connexion sans mot de passe via l'authenticateur du navigateur (Touch ID, Windows Hello, clé FIDO2, etc.).

**Enrôler une clé d'accès :**
1. Se connecter avec login / mot de passe
2. Cliquer sur **🔑 Mes clés** dans la barre de navigation
3. Saisir un libellé pour la clé et cliquer sur *Enregistrer une nouvelle clé*

**Se connecter avec une passkey :**
- Sur la page `/login`, cliquer sur **🔑 Se connecter avec une clé d'accès (Passkey)**

**Configuration locale** (défaut) : fonctionne sur `http://localhost:8080` sans configuration supplémentaire.

**Configuration production** : nécessite HTTPS et les variables `WEBAUTHN_RP_ID` / `WEBAUTHN_ALLOWED_ORIGINS`.

## Rôles et droits

| Action | VIEWER | EDITOR | ADMIN |
|--------|--------|--------|-------|
| Voir le budget | ✅ | ✅ | ✅ |
| Modifier les dépenses récurrentes | ❌ | ✅ | ✅ |
| Voir l'épargne | ✅ | ✅ | ✅ |
| Actualiser les soldes épargne | ❌ | ✅ | ✅ |
| Administration (tout) | ❌ | ❌ | ✅ |

## Export API

```bash
curl -H "X-Api-Key: <votre-clé>" http://localhost:8080/api/export
```

Retourne un fichier JSON complet. Les clés API se gèrent depuis Administration > Clés API.

## Tests
  
```bash
./mvnw verify
```

## Contribution

1. Fork du projet
2. Créer une branche feature (`git checkout -b feature/ma-feature`)
3. Committer les changements (`git commit -m "feat: ma feature"`)
4. Mettre à jour `CHANGELOG.md`
5. Ouvrir une Pull Request
