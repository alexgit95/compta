# Changelog

Toutes les modifications notables de ce projet sont documentées dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/),
et ce projet adhère au [Versionnement Sémantique](https://semver.org/lang/fr/).

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
