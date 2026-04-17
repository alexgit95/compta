# Changelog

Toutes les modifications notables de ce projet sont documentées dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/),
et ce projet adhère au [Versionnement Sémantique](https://semver.org/lang/fr/).

## [0.3.9]

### Ajouté

- **Objectifs** : deux options pour le calcul de la date estimée d'atteinte d'un objectif de solde cible :
  - **Tendance** : utilise une régression linéaire sur les derniers mois (par défaut, correspond à la courbe affichée).
  - **Projection** : utilise une simple moyenne arithmétique de la croissance mensuelle (méthode héritée).
  - Les deux dates sont affichées comme des boutons radio dans l'onglet Objectifs pour permettre à l'utilisateur de choisir sa méthode préférée.
  
- **Symbole infini** : lorsqu'une date estimée dépasse 25 ans dans le futur, elle est affichée sous la forme du symbole `∞` au lieu d'une date, pour indiquer une atteinte très lointaine ou improbable.
  - Implémentation en JavaScript côté client pour le formatage dynamique des dates.
  - Basé sur `estimatedReachDateByProjection()` dans `GoalService` et mise à jour du template `goals.html` avec les deux options.

### Modifié

- **`GoalService`** : 
  - Nouvelle méthode `estimatedReachDateByProjection()` pour calculer la date estimée par projection (moyenne simple).
  - Nouvelle classe interne `EstimatedReachDates` pour encapsuler les deux dates (tendance et projection).
  - Nouvelle méthode `estimatedReachDates()` qui retourne les deux dates pour un objectif.

- **`GoalController`** : ajustement du mapping pour passer les deux dates estimées (tendance et projection) au template via un `Map<String, LocalDate>` par objectif.

- **`goals.html`** (template) :
  - Affichage des deux options de date estimée avec des boutons radio.
  - Ajout d'une classe CSS `estimate-date-display` et de data-attributes pour le formatage dynamique des dates.
  - Ajout de code JavaScript (`isDateTooFar()`, `formatEstimateDate()`) pour afficher `∞` si la date dépasse 25 ans.

## [0.3.8]

### Corrigé

- **Objectifs** : correctif courbe tendance pour objectif (calcul date estimée erroné).

## [0.3.7]

### Ajouté

- **Alerte épargne non mise à jour** : un bandeau d'avertissement s'affiche en haut de l'onglet Épargne lorsqu'un ou plusieurs comptes n'ont pas reçu de nouvelle saisie depuis plus de 30 jours.
  - La liste des comptes concernés est affichée avec la date de la dernière saisie (ou « aucune saisie » si aucune entrée n'existe).
  - Calcul réalisé côté serveur dans `SavingsController` via un attribut `staleAccounts` passé au modèle Thymeleaf.
  - Affichage conditionnel dans `savings.html` uniquement lorsque la liste est non vide.

## [0.3.6]

### Ajouté

- **Gestion du fuseau horaire** : le fuseau horaire de l'application est maintenant configurable via la variable d'environnement `APP_TIMEZONE` (format IANA, ex: `Europe/Paris`). Cela corrige le décalage horaire constaté sur les données (heure affichée vs heure de saisie réelle).
  - `DemoApplication.java` : `TimeZone.setDefault` appliqué au démarrage de la JVM depuis `APP_TIMEZONE`.
  - `application.properties` : `spring.jackson.time-zone` et `spring.jpa.properties.hibernate.jdbc.time_zone` positionnés sur `${APP_TIMEZONE:Europe/Paris}`.
  - `Dockerfile` : variable `ENV APP_TIMEZONE=Europe/Paris` et argument JVM `-Duser.timezone=Europe/Paris` ajoutés.
  - Valeur par défaut : `Europe/Paris` (gère automatiquement le passage heure d'hiver/heure d'été).

- **Page d'enrôlement WebAuthn stylisée** (`GET /webauthn/register`) : la page de gestion des clés d'accès (Passkeys) est désormais rendue via un template Thymeleaf intégré dans le design de l'application (navbar, cartes, tableaux, couleurs CSS) au lieu de la page par défaut générée par Spring Security.
  - Affichage de la liste des clés existantes (nom, date de création, dernière utilisation) avec bouton de suppression.
  - Formulaire d'enregistrement d'une nouvelle clé avec champ de libellé.
  - Messages de succès / erreur gérés par `spring-security-webauthn.js` (compatibilité totale avec les endpoints Spring Security existants).
  - Bloc informatif sur les avantages des Passkeys.
  - Nouveaux fichiers : `PasskeyPageFilter.java`, `PasskeyDto.java`, `templates/webauthn/register.html`.

## [0.3.5]

### Corrigé

- **WebAuthn / Passkeys PostgreSQL** : correction du type de colonne pour les données binaires dans la table `user_credentials`. Les colonnes `public_key`, `attestation_object` et `attestation_client_data_json` sont maintenant explicitement définies comme `bytea` au lieu de `oid`.

## [0.3.4] - 2026-04-13

### Ajouté

- **Plein écran sur les graphiques** : tous les graphiques de l’application (Épargne, Budget, Objectifs) sont désormais cliquables. Un clic sur le graphique l'affiche en plein écran sur fond sombre, idéal pour une consultation mobile.
  - Icône d’agrandissement (↗↙) affichée en incrustation en haut à droite de chaque graphique pour indiquer l’interaction possible.
  - Bouton **×** (coin haut-droit) et touche **Échap** pour quitter le plein écran.
- **Zoom / défilement en plein écran** : en mode plein écran, il est possible de zoomer sur l’axe horizontal (temps) et de naviguer dans la période :
  - **Molette de souris** : zoom avant / arrière.
  - **Pincer (touch)** : zoom à deux doigts sur mobile / tablette.
  - **Glisser** : déplacer la vue après zoom.
  - Bouton **🔍 Reset** pour revenir à la vue complète.
- **Infrastructure partagée** : l’overlay plein écran, les scripts `hammerjs` et `chartjs-plugin-zoom` sont désormais chargés une seule fois dans `layout.html`. Chaque page enregistre son ou ses graphiques via `fsChartBuilders`.

## [0.3.3] - 2026-04-13

### Amélioré

- **Épargne – modes d'affichage du graphique « Évolution globale »** : ajout de trois boutons de sélection du mode de visualisation directement dans l'entête du graphique :
  - **Réelles** (mode par défaut) : affiche uniquement la courbe reliant les valeurs réelles saisies.
  - **+ Projection** : affiche les valeurs réelles et la courbe de projection mensuelle (versements automatiques, tirets).
  - **+ Tendance** : affiche les valeurs réelles et la droite de tendance linéaire calculée sur les 2 dernières années (pointillés).
  Le bouton actif est mis en évidence (`btn-primary`), les autres restent en style secondaire (`btn-outline`).

## [0.3.2] - 2026-04-13

### Amélioré

- **PWA / Mobile – barre de navigation responsive** : ajout d'un bouton hamburger (☰) dans la barre de navigation. Sur les écrans ≤ 768 px, les liens sont masqués par défaut et s'affichent en colonne plein-largeur au clic, supprimant le défilement horizontal.
- **Graphiques objectifs – hauteur augmentée** : les graphiques de tendance par objectif (courbes Valeurs réelles, Projection, Tendance) passent de `260 px` à `380 px` de hauteur (`420 px` sur mobile) pour améliorer la lisibilité des courbes.
- **Graphiques objectifs – axe Y normalisé** : l'axe Y de chaque graphique de tendance (section « Objectifs de solde ») démarre maintenant à `0` et se termine à la valeur maximale présente dans le graphique (soldes réels, projection, tendance et ligne d'objectif) multipliée par `1,15`, garantissant une vue lisible et cohérente quelle que soit l'amplitude des données.

## [0.3.1] - 2026-04-13

### Corrigé

- **Bouton déconnexion** : le bouton de la barre de navigation affichait le nom de l'utilisateur connecté ; il affiche désormais « Déconnexion » pour une meilleure lisibilité.
- **Graphique budget** : correction d'une erreur JavaScript `Uncaught SyntaxError: Unexpected token '{'` due à la sérialisation Thymeleaf des clés `LocalDate` (sérialisées en objet Java au lieu d'une chaîne ISO). Les clés de la map sont désormais converties en `String` (`LocalDate.toString()`) dans le contrôleur avant passage au modèle.
- **Passkey / WebAuthn – domaine configurable via Portainer** : la variable d'environnement `WEBAUTHN_RP_ID` (déjà supportée) est désormais documentée explicitement comme paramètre obligatoire à configurer dans Portainer pour corriger l'erreur `'rp.id' cannot be used with the current origin`. Voir le README pour les détails de configuration.



### Ajouté

#### Authentification par clé d'accès (Passkeys / WebAuthn)

- **Dépendance `spring-security-webauthn`** : ajout du module officiel Spring Security 7.0.4 WebAuthn (géré par le BOM Spring Boot 4.0.5, aucune version à épingler).
- **Schéma de base de données** : deux nouvelles tables (`user_entities`, `user_credentials`) créées automatiquement par Hibernate via les entités JPA `UserEntityRecord` et `UserCredentialRecord`. Le schéma est compatible SQLite (local) et PostgreSQL (production).
- **`WebAuthnConfig`** : bean de configuration déclarant les repositories JDBC officiels de Spring Security :
  - `JdbcPublicKeyCredentialUserEntityRepository` (table `user_entities`)
  - `JdbcUserCredentialRepository` (table `user_credentials`)
- **`SecurityConfig`** : activation du configurer `.webAuthn()` avec les paramètres `rpName`, `rpId` et `allowedOrigins` injectés depuis les variables d'environnement. Spring Security génère automatiquement les endpoints :
  - `POST /webauthn/authenticate/options` — challenge d'authentification
  - `POST /login/webauthn` — vérification de l'assertion
  - `POST /webauthn/register/options` — challenge d'enrôlement
  - `POST /webauthn/register` — enregistrement de la clé
  - `GET /webauthn/register` — page de gestion des clés (générée par Spring Security)
  - `DELETE /webauthn/register/{credentialId}` — suppression d'une clé
- **Script client `spring-security-webauthn.js`** : copie du script officiel fourni dans le jar Spring Security, exposé sous `/js/spring-security-webauthn.js`.
- **Page de connexion** (`login.html`) : ajout du bouton *"🔑 Se connecter avec une clé d'accès (Passkey)"* avec gestion du token CSRF, désactivation gracieuse si le navigateur ne supporte pas WebAuthn.
- **Navigation** (`layout.html`) : ajout du lien *"🔑 Mes clés"* → `/webauthn/register` pour gérer les clés d'accès enregistrées.
- **Variables d'environnement WebAuthn** dans `application.properties` et `application-prod.properties` :

  | Variable | Description | Défaut |
  |---|---|---|
  | `WEBAUTHN_RP_NAME` | Nom affiché dans le prompt du navigateur | `Budget App` |
  | `WEBAUTHN_RP_ID` | Domaine de confiance (sans port) | `localhost` |
  | `WEBAUTHN_ALLOWED_ORIGINS` | Origines autorisées (virgule-séparées) | `http://localhost:8080` |

### Notes de déploiement

> **WebAuthn requiert HTTPS en production** (sauf `localhost`). Sur le Raspberry Pi, configurer un reverse proxy TLS (Traefik ou Nginx + Let's Encrypt) avant d'activer les passkeys.
> Variables à ajouter dans Portainer : `WEBAUTHN_RP_ID=votre-domaine.com` et `WEBAUTHN_ALLOWED_ORIGINS=https://votre-domaine.com`.

## [0.2.0] - 2026-04-09

### Ajouté

#### Module Objectifs (nouveau)

- **Nouveau modèle `Goal`** (`goal` en base) : objectif lié à un compte épargne, de type `TARGET_BALANCE` (solde cible) ou `MONTHLY_CONTRIBUTION` (versement mensuel cible), avec un montant objectif.
- **`GoalRepository`**, **`GoalService`**, **`GoalController`** : stack complète CRUD pour les objectifs.
- **Page `/goals`** (onglet *Objectifs* dans la navigation) :
  - **Sélecteur de tendance** : champ numérique pour choisir sur combien d'années analyser la tendance (1–20, défaut 3).
  - **Histogramme "Versements mensuels"** : barre *versement actuel* vs *objectif* par compte (visible si au moins un objectif de type `MONTHLY_CONTRIBUTION`).
  - **Histogramme "Solde cible"** : barre *solde actuel* vs *objectif* par compte (visible si au moins un objectif `TARGET_BALANCE`), avec infobulle indiquant la date d'atteinte estimée.
  - **Courbe de tendance par objectif de solde** : graphique ligne couvrant `trendYears` années passées jusqu'à la date d'atteinte estimée (ou +5 ans), avec régression linéaire, projection mensuelle (tirets), ligne horizontale de l'objectif (annotation rouge pointillée) et marqueur *Aujourd'hui*.
  - **Date d'atteinte estimée** : calculée à partir du taux moyen de croissance mensuelle sur la période de tendance ; affichée au format `MM/yyyy` dans l'en-tête de chaque courbe de tendance.
  - **Bandeau d'alerte** (⚠️) : affiché en haut de la page pour chaque compte dont le solde a atteint l'objectif mais dont le versement mensuel est encore actif.
  - **Tableau récapitulatif** : liste de tous les objectifs avec progression en % (colorée en vert à 100 %).
  - **Modal de création d'objectif** (EDITOR/ADMIN uniquement) : sélection du compte, du type et du montant.
  - **Suppression d'objectif** (EDITOR/ADMIN uniquement) avec confirmation.
- **Import/Export** : les objectifs sont inclus dans le JSON exporté (`goals`) et restaurés lors de l'import (re-liaison par libellé de compte). L'ordre de suppression est respecté (goals supprimés avant les comptes épargne).
- **Sécurité** : `/goals/**` accessible en lecture à tous les rôles (`ADMIN`, `EDITOR`, `VIEWER`) ; création et suppression restreintes à `EDITOR` et `ADMIN` via `@PreAuthorize`.

## [0.1.3] - 2026-04-09

### Modifié

#### Module Épargne
- **Ajustement de l'échelle Y du graphique** : l'axe Y du graphique d'épargne suggère désormais un maximum supérieur à la valeur maximale des séries (marge de ~10%) pour améliorer la lisibilité. Fichier modifié : `src/main/resources/templates/savings.html`.

## [0.1.2] - 2026-04-08

### Ajouté

#### Module Épargne
- **Suppression de valeurs historiques** : bouton 🗑️ sur chaque ligne de l'historique dans la page d'édition d'un compte (`savings-edit.html`), endpoint `POST /savings/entries/{id}/delete` sécurisé ADMIN/EDITOR avec confirmation
- **Tableau de variation sur une période** : nouvelle section dans l'onglet épargne avec sélecteurs "Du / Au" (défaut : aujourd'hui - 1 an → aujourd'hui), affichant pour chaque compte le solde de début, de fin, la variation en euros et en pourcentage, avec ligne de total et coloration vert/rouge
- **Dernière valeur connue** dans les cartes de compte : affiche le montant réel et la date de la dernière saisie, sous la valeur estimée
- **Plage par défaut des sélecteurs** : graphique ±6 mois autour d'aujourd'hui ; tableau de variation aujourd'hui - 1 an → aujourd'hui

#### Module Budget
- **Solde par défaut** : pré-rempli automatiquement avec la somme des dépenses récurrentes à venir dans le mois + 700 €
- **Axe Y du graphique** : démarre à 0 par défaut, descend en négatif si une valeur projetée passe en dessous

### Corrigé

#### Module Épargne
- **Décalage d'un mois sur la projection** : `projectBalance()` compare désormais les dates au 1er du mois (`withDayOfMonth(1)`) pour éviter qu'une entrée saisie le 8 soit ignorée lors du calcul du 1er du même mois
- **Désordre de l'axe des dates** (`2026-04` avant `2025-05`) : `buildChart()` construit un tableau `labels` trié (`Array.from(Set).sort()`) commun à tous les datasets avant de les passer à Chart.js
- **Mismatch JS** `p.value`/`p.isEntry` → `p.balance`/`p.real` dans le graphique épargne

#### Module Administration
- **`users.html`** : balise `<span>` malformée (attribut `th:text` rendu en texte littéral suite à une mauvaise correction précédente du `th:class`)
- **Doublon de code** dans `SavingsController` : `return "savings-edit";` dupliqué après l'ajout de `deleteEntry`, provoquant une erreur de compilation

### Modifié

#### Module Épargne — graphique
- **3 datasets distincts par compte** : valeurs réelles (ligne pleine + grands points), projection mensuelle (tirets), tendance linéaire 2 ans (pointillés), chacun dans sa propre couleur issue d'une palette dédiée
- **Régression linéaire** calculée côté JavaScript sur les valeurs réelles des 2 dernières années, projetée sur toute la plage visible
- **Génération mensuelle des points** : `SavingsService.getChartData()` produit un point par mois sur la plage complète au lieu de ne retourner que les entrées réelles

## [0.1.1] - 2026-04-08

### Corrigé

###  Ajouté (2026-04-09)

- **PWA** : ajout d'un `manifest.webmanifest`, icônes et `sw.js` pour permettre l'installation sur Android et le mode hors-ligne. Les ressources publiques sont exposées via `/manifest.webmanifest`, `/icons/**` et `/sw.js`.
#### Sécurité / Spring Boot
- **Dépendance circulaire** entre `SecurityConfig`, `ApiKeyAuthFilter` et `ApiKeyService` : déplacement du bean `PasswordEncoder` dans une classe dédiée `PasswordEncoderConfig`
- **Import de données** (`ImportExportService`) : remplacement de `deleteAll()` par `deleteAllInBatch()` suivi de `entityManager.flush()` + `entityManager.clear()` pour éviter la violation de contrainte UNIQUE lors de la réimportation

#### Thymeleaf 3.1 — incompatibilités corrigées
- **`budget.html`** : correction de l'apostrophe dans SpEL (`'Projection jusqu\'au'` → literal template `|...|`), correction du mélange `th:class="'literal' + ${...}"`, suppression des lambdas Java non supportées (`(a,b) -> b`) remplacées par des attributs calculés dans le contrôleur (`endOfMonthBalance`, `totalMonthExpenses`, `endOfMonth`), correction de la concaténation `€` hors expression `${}`
- **`categories.html`** : remplacement de `th:onclick` (interdit pour les chaînes) par des attributs `data-*` lus via `btn.dataset` en JavaScript
- **`data.html`** : remplacement de `${#request.scheme + ...}` (objet `#request` interdit) par une variable `apiBaseUrl` construite dans le contrôleur
- **`savings.html`** : correction du mismatch `p.value`/`p.isEntry` → `p.balance`/`p.real`, correction de la concaténation `€` hors expression `${}`
- **`savings-edit.html`** : correction du mélange `'Modifier : ' + ${...}` → literal template, correction de la concaténation `€`
- **`users.html`** : correction du mélange `th:class="'badge badge-' + ${...}"` → encapsulé dans `${}`

#### Configuration VS Code
- Ajout de `.vscode/settings.json` avec `maven.executable.path`, `maven.settingsFile` et `java.import.maven.enabled` pour pointer vers l'installation Maven interne
- Ajout de `-s C:\USINE_LOGICIELLE\apache-maven\conf\settings.xml` dans `mvnw.cmd` pour forcer l'usage du fichier de configuration d'entreprise

### Ajouté

#### Module Épargne
- **Graphique avec plage de dates configurable** : deux sélecteurs `<input type="month">` (Du / Au) permettant de filtrer la plage affichée ; la borne minimale est la date de la première valeur saisie, la borne maximale est aujourd'hui + 2 ans
- **Génération mensuelle des points** : `SavingsService.getChartData()` reworké pour générer un point par mois sur toute la plage, avec distinction points réels (grande taille) vs projetés (petite taille)
- **Filtrage côté client** : le graphique se redessine sans rechargement à chaque changement de plage
- **Contrôleur** (`SavingsController`) : calcul automatique des bornes globales (`chartMinDate`, `chartMaxDate`) passées au template

## [0.1.0] - 2026-04-08

### Ajouté

#### Fonctionnalités principales
- **Module Budget** : saisie du solde courant et projection journalière jusqu'en fin de mois avec graphique Chart.js
- **Dépenses récurrentes** : CRUD complet (libellé, montant, catégorie, jour du mois)
- **Module Épargne** : gestion de plusieurs comptes, saisie de valeurs de référence, simulation de projection, graphique multi-comptes avec points de valeur réelle
- **Export via API REST** : endpoint `GET /api/export` protégé par clé API (header `X-Api-Key` ou `Authorization: ApiKey`)

#### Administration (rôle ADMIN uniquement)
- Gestion des catégories de dépenses avec icônes emoji
- Gestion des utilisateurs (3 rôles : ADMIN, EDITOR, VIEWER) avec génération automatique de mot de passe
- Gestion des clés API : création avec nom et durée de validité, révocation, historique de dernière utilisation
- Import / Export JSON complet de la base de données

#### Sécurité
- Authentification par login / mot de passe
- Remember-me 12 mois
- Protection CSRF sur tous les formulaires
- Credentials administrateur via variables d'environnement (`ADMIN_USERNAME`, `ADMIN_PASSWORD`)
- Clés API avec hash BCrypt (la valeur brute n'est jamais stockée)
- Utilisateur Docker non-root

#### Infrastructure
- Profil `local` avec SQLite (développement)
- Profil `prod` avec PostgreSQL (production, compatible ARM64)
- Dockerfile multi-stage avec image Eclipse Temurin 17
- GitHub Actions : build Maven + tests + image multi-plateforme (`amd64`/`arm64`) + push Docker Hub
- Interface web Thymeleaf avec CSS responsive

#### Stack technique
- Spring Boot 3.4.4 / Java 17
- Spring Security 6 / Spring Data JPA
- Thymeleaf + thymeleaf-extras-springsecurity6
- SQLite 3.47 (Hibernate Community Dialects) / PostgreSQL 15
- Chart.js 4 (CDN)
- Lombok / Jackson
