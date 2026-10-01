# Budget App

Application Spring Boot de gestion de budget personnel et d'épargne, avec interface web Thymeleaf.

## Fonctionnalités

- **Budget** : saisie du solde courant, projection quotidienne jusqu'en fin de mois avec graphique d'évolution
  - Gestion des courses : configuration du montant des courses et de leur fréquence (nombre de jours entre chaque achat)
  - Affichage du nombre de courses restantes ce mois et du montant restant à dépenser en courses
  - Prévision des dépenses de courses basée sur la date courante et la dernière date d'achat
  - **Décomposition des dépenses** : la carte "Dépenses restantes ce mois" affiche le montant total avec ventilation entre dépenses récurrentes et courses
- **Dépenses récurrentes** : gestion des dépenses mensuelles (catégorie, libellé, montant, jour du mois)
- **Prévisionnel (BETA)** : analyse à la demande des débits récurrents des trois derniers mois importés consécutifs terminés, avec tableau des charges candidates mensuelles et hebdomadaires
- **Épargne** : suivi de plusieurs comptes épargne avec simulation de progression et graphiques
  - Catégorie par compte : 🔄 **Fond de roulement** (livrets, dépenses courantes) ou 📈 **Épargne long terme** (prise en compte dans le patrimoine)
  - Deux graphiques séparés partageant les mêmes contrôles (mode réelles / + projection / + tendance 2 ans, plage de dates, plein écran) :
    - 🔄 Livrets & fond de roulement
    - 📈 Épargne long terme
  - Tableau de variation sur une période : plage de dates partagée, deux sous-tableaux par catégorie (solde début, solde fin, variation €)
  - Typage des comptes par type de support (Livret, PEA, Assurance Vie, SCPI, Crypto…) avec icônes
  - Diagramme en donut de la répartition par type de support
  - Conseil de répartition : comparaison allocation actuelle vs recommandée
  - Indicateur d'épargne de précaution (objectif 3 à 6 mois de revenus en liquidités)
- **Objectifs** : définition d'objectifs de solde cible ou de versement mensuel par compte épargne :
  - Histogramme comparant versements actuels vs objectifs de versement mensuel
  - Histogramme synthèse solde actuel vs solde objectif + courbes de tendance par compte
  - Calcul automatique de la date d'atteinte de l'objectif selon la tendance observée sur X années (paramétrable)
  - Alerte automatique si un objectif de solde est atteint mais que des versements sont encore actifs
  - Accès lecture VIEWER/EDITOR/ADMIN, modification réservée EDITOR et ADMIN
- **Crédits** : suivi de tous vos crédits en cours (immobilier, automobile, consommation, travaux, étudiant, autre)
  - Ajout / modification / suppression de crédits avec type, montant, taux, dates, mensualité et montant restant
  - Tableau récapitulatif avec barre de progression du remboursement (%) et durée restante (années/mois)
  - Cartes synthèse : total mensualités, total restant dû, nombre de crédits
  - Rattachement optionnel d'un crédit à un bien immobilier déclaré en administration
  - Prise en charge complète dans l'import/export JSON
- **Patrimoine** : vue consolidée de votre patrimoine financier
  - 4 indicateurs synthétiques : Patrimoine brut, Patrimoine net, Valeur immobilière, Valeur du capital (épargne long terme)
  - Patrimoine brut = valeur actuelle des biens immobiliers + somme des comptes épargne catégorisés **📈 Épargne long terme**
  - Patrimoine net = patrimoine brut − restant dû de tous les crédits
  - La catégorie (🔄 Fond de roulement / 📈 Épargne long terme) se configure **compte par compte** dans Épargne → ✏️ Modifier le compte
  - Graphique d'évolution dans le temps (patrimoine brut et net) basé sur l'amortissement des crédits et la tendance d'épargne des N derniers mois (régression linéaire) ou les versements mensuels (projection)
  - Tableau de projections : patrimoine brut et net estimé à horizon 6 mois, 1 an et 5 ans avec évolution nette
  - Support plein écran avec zoom et déplacement
- **Administration** (réservée ADMIN) :
  - Gestion des catégories avec icônes emoji
  - Gestion des biens immobiliers (libellé, valeur d'achat, date d'achat, valeur actuelle sur le marché)
  - Gestion des types de support d'épargne avec icônes et pourcentages recommandés
  - Gestion des courses : configuration du montant et de la fréquence avec affichage des prévisions
  - Gestion des utilisateurs (3 rôles : ADMIN, EDITOR, VIEWER)
  - Clés API avec nom, durée de validité et historique d'utilisation
  - Import / Export JSON de toute la base de données (incluant biens immobiliers, liaisons crédit-bien et configuration des courses)
  - Import par lot de relevés bancaires CSV et conservation de l'historique des opérations
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

### Importer des relevés bancaires

Dans **Administration > Import / Export**, choisir le mois, l'année et un fichier CSV ; le bouton **+** permet d'ajouter d'autres lignes (jusqu'à dix relevés). Retirer une ligne avant de cliquer sur **Importer les relevés** si besoin. Chaque mois est traité indépendamment : un fichier invalide laisse son mois intact sans empêcher les autres imports. Deux lignes du même lot visant le même mois échouent toutes les deux. Un bilan de succès ou d'erreur par ligne apparaît après l'envoi, sans journal permanent des tentatives.

Les mois enregistrés et leur nombre d'opérations apparaissent sur la même page ; cliquer sur un mois pour consulter ses lignes. Un nouvel import remplace **tout le mois choisi**, sans toucher aux autres mois. Le bouton de suppression à côté d'un mois efface, après confirmation, **toutes ses lignes**, opérations et anciens soldes compris.

Le format pris en charge est celui des relevés d'exemple dans `docs/` : UTF-8 (avec ou sans BOM), sans en-tête, huit champs séparés par `;` pour les opérations, quatre ou huit pour les lignes de solde, date `jj/MM/aaaa` et montant signé avec virgule décimale. Les lignes de solde sont lues mais ne sont plus enregistrées lors des nouveaux imports ; celles déjà stockées restent présentes jusqu'à la réimportation ou à la suppression manuelle du mois. Débits, crédits et virements restent dans l'historique sans classement automatique. Taille maximale : 2 Mo par fichier, dix relevés par lot. Les dates doivent toutes correspondre au mois et à l'année sélectionnés.

L'export JSON global inclut ces relevés. La restauration JSON remplace **toutes** les données, y compris cet historique ; restaurer une ancienne sauvegarde sans relevés efface donc les relevés déjà importés. L'import CSV mensuel est indépendant de cette restauration globale.

### Analyser les charges fixes

Dans **Prévisionnel (BETA)**, choisir au besoin la **tolérance de montant** (0 à 10 %, par pas de 0,5 ; défaut 4 %), puis cliquer sur **Analyser les charges fixes**. Ce même réglage sert à la détection et au rapprochement Budget : il est conservé avec le dernier résultat, sans devenir une préférence globale. L'application choisit la série de trois mois importés consécutifs la plus récente avant le mois en cours ; en cas de lacune récente, elle utilise une série antérieure si elle existe. Si aucune série n'est disponible, elle signale l'historique insuffisant.

Le dernier résultat est enregistré en base et retrouvé à l'ouverture de l'onglet, sans relancer la détection. Un réimport ou une suppression d'un des relevés analysés l'invalide ; une période de trois mois devenue différente le rend également périmé. L'écran affiche alors **Analyse à relancer** et masque les anciennes charges, sans recalcul automatique. L'export/restauration JSON global inclut ce résultat ; une ancienne sauvegarde sans analyse laisse l'écran prêt pour un premier calcul.

Le tableau montre les débits candidats, leur montant habituel, leur fréquence mensuelle ou hebdomadaire et les dates observées. Pour une série mensuelle avec plusieurs débits identiques chaque mois, le montant est le total mensuel habituel et toutes les occurrences sont comptées (deux Navigo de 90,80 € donnent six occurrences et 181,60 € par mois sur trois mois). Pour une série hebdomadaire, le montant reste celui d'une occurrence. Le total en pied de tableau additionne uniquement les charges mensuelles détectées, même si elles sont déjà dans Budget ; les hebdomadaires en sont exclues. Le tableau est trié par numéro du jour de la première occurrence, puis par libellé et montant en cas d'égalité. Les virements sortants vers l'épargne sont inclus, les crédits et les anciennes lignes de solde sont ignorés. Le rapprochement utilise d'abord le libellé, des montants proches selon la tolérance choisie et des dates régulières. Pour les seuls virements dont le libellé change, un second passage privilégie le montant et la cadence si le cycle n'est pas ambigu ; le premier libellé observé illustre alors la ligne, suivi de « (libellé variable) ».

La colonne **Budget** rapproche les montants mensuels détectés des dépenses récurrentes saisies, puis compare leur jour habituel à ±2 jours sans imposer le même libellé : **Retrouvée** si la correspondance est unique, **À vérifier** en cas de montant partiel, de jour éloigné ou d'ambiguïté, **Non retrouvée** sans montant correspondant et **Non comparable** pour une charge hebdomadaire. Sous le tableau, les dépenses Budget non confirmées sont listées comme **Correspondance incertaine** si elles sont seulement « À vérifier », ou **Non identifiée dans les charges fixes** si aucune candidate ne les rapproche. Cette liste et les statuts Budget utilisent toujours les dépenses actuelles, même quand le résultat d'analyse a été enregistré auparavant. Les rôles ADMIN/EDITOR peuvent choisir **Ajouter** sur une candidate mensuelle non retrouvée ou à vérifier : le formulaire Budget est prérempli, mais ne crée rien avant confirmation et contrôle antidoublon. Une série de deux Navigo peut correspondre à deux charges Budget de 90,80 € ou une charge de 181,60 € ; si un seul paiement existe déjà, ajouter directement l'agrégat serait refusé pour éviter un double comptage. Ces candidats BETA peuvent manquer des charges ou produire de faux positifs et ne calculent pas encore le solde de fin de mois ni Holt-Winters.

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
| `WEBAUTHN_RP_ID` | Domaine WebAuthn **sans port** (ex: `budget.mondomaine.com`) | `localhost` |
| `WEBAUTHN_ALLOWED_ORIGINS` | Origines autorisées virgule-séparées (ex: `https://budget.mondomaine.com`) | `http://localhost:8080` |
| `WEBAUTHN_RP_NAME` | Nom affiché dans le prompt passkey | `Budget App` |
| `APP_TIMEZONE` | Fuseau horaire de l'application (format IANA, ex: `Europe/Paris`) | `Europe/Paris` |

> ⚠️ **WebAuthn requiert HTTPS en production.** Sur le Raspberry Pi, placez un reverse proxy TLS devant l'application (Traefik ou Nginx + Let's Encrypt) et configurez `WEBAUTHN_RP_ID` avec votre domaine réel.
>
> ⚠️ **Erreur `'rp.id' cannot be used with the current origin`** : cette erreur signifie que `WEBAUTHN_RP_ID` ne correspond pas au domaine depuis lequel vous accédez à l'application. `WEBAUTHN_RP_ID` doit être exactement le nom d'hôte (sans `https://` ni port), et `WEBAUTHN_ALLOWED_ORIGINS` doit contenir l'URL complète avec le schéma. Exemple pour `budget.mondomaine.com` :
> ```
> WEBAUTHN_RP_ID=budget.mondomaine.com
> WEBAUTHN_ALLOWED_ORIGINS=https://budget.mondomaine.com
> ```

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
      APP_TIMEZONE: Europe/Paris
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
