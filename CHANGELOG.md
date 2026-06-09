# Changelog

Toutes les modifications notables de ce projet sont documentées dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/),
et ce projet adhère au [Versionnement Sémantique](https://semver.org/lang/fr/).

## [0.8.0] - 2026-06-09

### Ajouté

- **Diagramme de Sankey – Budget** : un diagramme de flux (Sankey) est affiché en bas de la page Budget. Il représente le flux des dépenses récurrentes depuis la source "Salaires" vers chaque catégorie (niveau 2) puis vers chaque dépense individuelle (niveau 3). Une catégorie spéciale "🛒 Courses" est incluse avec un montant configurable.
- **Diagramme de Sankey – Épargne** : un diagramme de flux est affiché en bas de la page Épargne, montrant la répartition des versements mensuels entre chaque compte épargne.
- **Paramètres applicatifs (`AppSetting`)** : nouvelle entité JPA `app_setting` (clé/valeur) permettant de stocker des paramètres configurables (salaire mensuel total, budget courses).
- **Page d'administration "Paramètres"** (`/admin/settings`) : formulaire permettant de configurer le salaire mensuel total et le budget courses utilisés dans le diagramme Sankey du budget.
- **Import/Export** : les `AppSetting` sont inclus dans l'export JSON et restaurés à l'import.
- Navigation Admin : lien "⚙️ Paramètres" ajouté dans la sous-navigation de toutes les pages d'administration.

## [0.7.5] - 2026-06-01

### Corrigé

- **WebAuthn / Passkey – bug 3 tentatives** : la cinématique (1ère tentative → erreur identifiants, 2ème → page blanche status 999, 3ème → succès) était causée par la gestion d'erreur JS qui effectuait un rechargement complet de page (`window.location.href = '/login?error'`) à chaque échec. Ce rechargement invalide la session et le token CSRF (Spring Security 7 + `XorCsrfTokenRequestAttributeHandler`), rendant les tentatives suivantes instables.
  - `spring-security-webauthn.js` : exposition de `window.webAuthnAuthenticate` (la fonction `authenticate()` interne) pour permettre une gestion fine côté login.
  - `login.html` : remplacement de `window.setupLogin()` par un gestionnaire d'événement personnalisé qui affiche une erreur *inline* en cas d'échec (sans rechargement de page), préserve le token CSRF et la session, et réactive immédiatement le bouton pour permettre une nouvelle tentative.
- **Journalisation WebAuthn/CSRF** : logs DEBUG ajoutés en profil local (`application-local.properties`) pour faciliter le diagnostic des erreurs WebAuthn.

## [0.7.4] - 2026-06-01

### Modifié

- **Épargne – Graphiques** : le graphique unique est scindé en deux graphiques distincts partageant les mêmes contrôles (mode réelles/projection/tendance + plage de dates) :
  - 🔄 **Livrets & fond de roulement** — comptes non marqués épargne long terme
  - 📈 **Épargne long terme** — comptes marqués long terme, chacun avec son propre axe Y adapté
- **Épargne – Tableau de variation** : la section "Variation sur une période" est unifiée dans une seule carte avec une plage de dates partagée et deux sous-tableaux (🔄 fond de roulement / 📈 long terme). Suppression du filtre par compte (devenu inutile avec la séparation par catégorie). Colonne "Variation (%)" retirée, contrôles simplifiés.
- **Épargne – Organisation de la page** : restructuration pour une lecture cohérente sur PC et mobile — graphiques, variations, répartition par type, conseils, puis fiches de compte.

## [0.7.3] - 2026-06-01

### Modifié

- **Épargne long terme – granularité unitaire** : le flag "épargne long terme" est maintenant porté par chaque **compte épargne** individuellement (et non plus par le type de support). Dans la page *Modifier le compte*, un nouveau sélecteur "Catégorie patrimoine" permet de choisir 🔄 Fond de roulement ou 📈 Épargne long terme pour chaque compte.
- **Admin Types de support** : suppression du champ catégorie (fond de roulement / long terme) qui n'est plus pertinent au niveau du type.
- **Patrimoine** : les calculs (capital, graphique, projections) filtrent désormais sur `SavingsAccount.longTermSavings` au lieu de `SavingsAccountType.longTermSavings`.

## [0.7.2] - 2026-06-01

### Ajouté

- **Administration – Types de support** : nouveau champ **catégorie** pour distinguer les types de comptes :
  - 🔄 **Fond de roulement** — dépenses courantes, travaux, voyages…
  - 📈 **Épargne long terme** — seuls ces comptes sont pris en compte dans le calcul du patrimoine.
  - La catégorie est affichée dans la liste des types avec badge coloré.
  - Les types par défaut sont pré-catégorisés (précaution et livret = fond de roulement ; PEA, fonds euros, SCPI, crypto = long terme).
- **Patrimoine – Filtrage épargne long terme** : les comptes de type "fond de roulement" sont exclus de tous les calculs de patrimoine (valeur du capital, graphique, projections).
- **Patrimoine – Note explicative** : rappel sous le graphique indiquant que seule l'épargne long terme est comptabilisée, avec lien direct vers l'administration.
- **Patrimoine – Tableau de projections** : nouveau tableau sous le graphique présentant le patrimoine brut et net projeté pour 4 horizons (Aujourd'hui, +6 mois, +1 an, +5 ans) avec l'évolution nette par rapport à la situation actuelle.

## [0.7.1] - 2026-06-01

### Ajouté

- **Patrimoine – Paramétrage de la projection** : sélecteur de mode pour le graphique d'évolution.

### Corrigé

- **Patrimoine – Mode Projection descendant** : le mode « Projection » pouvait afficher un patrimoine brut descendant parce que `projectBalance` projetait naïvement depuis d'anciennes entrées d'épargne avec les versements mensuels, créant un décalage lorsqu'une entrée plus récente affichait un solde inférieur. Le futur est maintenant ancré sur le solde total réel d'aujourd'hui + cumul des versements mensuels, garantissant une courbe toujours ascendante.
  - Mode **Tendance** : projection basée sur une régression linéaire de l'épargne passée.
  - Mode **Projection** : projection basée sur les versements mensuels programmés de chaque compte.
  - Choix de la durée d'historique pour la tendance : 6 mois, 1 an, 2 ans ou 5 ans.
  - Le sélecteur de durée s'affiche/masque dynamiquement selon le mode choisi.

## [0.7.0] - 2026-06-01

### Ajouté

- **Onglet Patrimoine** : nouvelle page accessible depuis la navigation principale.
  - 4 indicateurs synthétiques : Patrimoine brut, Patrimoine net, Valeur immobilière, Valeur du capital.
  - Graphique d'évolution (Chart.js) du patrimoine brut et net dans le temps, basé sur l'amortissement des crédits et la tendance d'épargne des 12 derniers mois (régression linéaire).
  - Support plein écran avec zoom/déplacement.
- **Administration – Biens immobiliers** : nouvelle sous-section pour déclarer ses biens immobiliers (libellé, valeur d'achat, date d'achat, valeur actuelle sur le marché).
- **Crédits – Rattachement à un bien immobilier** : possibilité d'associer un crédit à un bien immobilier déclaré dans l'administration.
- **Import/Export** : les biens immobiliers sont inclus dans l'export JSON et correctement ré-importés avec re-liaison des crédits.

## [0.6.1] - 2026-06-01

### Ajouté

- **Crédits – Graphique d'évolution du capital restant dû** : nouveau graphique en courbes (Chart.js) affiché au-dessus du tableau récapitulatif.
  - Une courbe par crédit (palette de couleurs distinctes) + une courbe **Total** en pointillés gris.
  - Projection mois par mois via la formule d'amortissement standard (intérêts = capital × taux annuel / 1200).
  - Chaque courbe **disparaît naturellement** une fois le capital remboursé (valeurs `null` transmises à Chart.js, qui interrompt le tracé).
  - La courbe Total s'arrête également dès que tous les crédits sont soldés.
  - Support plein écran (clic sur le graphique) avec zoom/déplacement.
  - Tooltip formaté en euros avec le détail par crédit.

### Corrigé

- **Crédits – Erreur Thymeleaf à l'affichage** : l'expression SpEL `T(java.math.BigDecimal).valueOf(75)` dans `th:style` provoquait une `SpelEvaluationException` (ambiguïté de méthode). La couleur de la barre de progression est désormais calculée côté serveur dans le contrôleur et transmise via un `Map<Long, String>`.

- **Crédits – Barre de progression invisible** : `th:style` remplaçait l'attribut `style` statique, supprimant `height:100%` et rendant la barre colorée de hauteur nulle. Tous les styles sont maintenant fusionnés dans le seul attribut `th:style`.

- **Crédits – Dates non pré-remplies en modification** : `th:field` utilisait le `ConversionService` pour formater les `LocalDate`, produisant un format localisé incompatible avec `<input type="date">` (qui attend `yyyy-MM-dd`).
  - Annotation `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` ajoutée sur les trois champs date de l'entité `Credit` (parsing lors du submit).
  - Remplacement de `th:field` par `th:value="${#temporals.format(..., 'yyyy-MM-dd')}"` dans le formulaire de modification.

## [0.6.0] - 2026-06-01

### Ajouté

- **Crédits – Nouvel onglet de suivi des crédits** : gestion complète des crédits en cours (immobilier, automobile, consommation, travaux, étudiant, autre).
  - Nouvelle entité `Credit` avec libellé, type, montant total, taux, date de début, date de fin, mensualité, montant restant et date du montant restant.
  - CRUD complet : ajout, modification, suppression de crédits.
  - Tableau récapitulatif avec barre de progression du pourcentage remboursé et durée restante (en années/mois).
  - Cartes synthèse affichant le total des mensualités, le total restant dû et le nombre de crédits.
  - Nouvel onglet 💳 Crédits dans la barre de navigation.

- **Import/Export – Prise en charge des crédits** : les crédits (`credits`) sont inclus dans l'export JSON et correctement restaurés lors de l'import.
  - Tests unitaires ajoutés : `importExportPreservesCreditsRoundTrip` vérifie un aller-retour complet export → import.
  - Tests existants mis à jour pour inclure les crédits dans les scénarios d'export et d'import.

- **Tests – CreditServiceTest** : tests unitaires dédiés au service de crédits (CRUD, calcul du pourcentage de remboursement, calcul de la durée restante).

## [0.5.1] - 2026-06-01

### Corrigé

- **Épargne – Types de support par défaut manquants** : les 6 types de support pré-configurés n'étaient insérés qu'à la condition `count() == 0`, ce qui empêchait leur création si la base de données existait déjà avec des enregistrements.
  - La logique d'initialisation vérifie désormais l'existence de chaque type **par son nom** (`findByName`) et n'insère que les types absents.
  - Les types par défaut (Épargne de précaution, Livret, Fonds euros, Actions/PEA, Immobilier, Crypto) sont ainsi toujours présents au démarrage, quelle que soit l'état préalable de la base.

- **Import/Export – Vérification de la conservation du type de support** : confirmation que le champ `accountType` de chaque `SavingsAccount` est bien sérialisé à l'export et correctement re-lié par nom à l'import.
  - Les 3 tests existants (`exportReturnsAllEntities`, `importDataClearsAndReimports`, `importExportPreservesAccountTypesRoundTrip`) ont été exécutés et passent tous avec succès.

## [0.5.0]

### Ajouté

- **Épargne – Typologie des comptes** : chaque compte épargne peut désormais être associé à un type de support (Livret, PEA, Assurance Vie, SCPI, Crypto, etc.).
  - Nouvelle entité `SavingsAccountType` avec nom, icône emoji et pourcentage de répartition recommandé.
  - 5 types de support pré-configurés au premier démarrage (Livret, Fonds euros, Actions/PEA, Immobilier, Crypto).
  - Les types sont entièrement paramétrables par l'administrateur (ajout, suppression) directement dans l'onglet Épargne.
  - Un badge avec l'icône du type s'affiche à côté du nom de chaque compte dans les cartes.

- **Épargne – Diagramme de répartition** : ajout d'un graphique en camembert (doughnut) montrant la distribution de l'épargne par type de support.
  - Tableau récapitulatif associé avec montants et pourcentages par type.

- **Épargne – Conseil de répartition & notation** : section de conseils en bas de l'onglet épargne.
  - Tableau comparatif allocation actuelle vs allocation recommandée (par type de support).
  - Indicateur d'écart (montant en surplus ou en déficit par type).
  - **Épargne de précaution** : jauge visuelle indiquant si l'épargne liquide couvre 3 à 6 mois de revenus, avec messages d'alerte ou de validation.

- **Import/Export – Prise en charge des types de comptes** : les types de support (`savingsAccountTypes`) sont inclus dans l'export JSON et correctement restaurés lors de l'import, y compris le lien entre chaque compte et son type.
  - Tests unitaires ajoutés : `importExportPreservesAccountTypesRoundTrip` vérifie un aller-retour complet export → import.

## [0.4.0]

### Sécurité

- **Dockerfile – utilisateur non-root** : correction de la finding Semgrep `missing-user-entrypoint`.
  - Création d'un groupe système `appgroup` et d'un utilisateur système `appuser` dans l'image finale.
  - Instruction `USER appuser` ajoutée avant `EXPOSE`, garantissant que le processus JVM ne s'exécute plus en tant que `root`.

### Ajouté

- **CI/CD – Analyse qualité Semgrep** : ajout d'un job `semgrep` dans la GitHub Action de build.
  - Exécuté en parallèle des tests via le container officiel `semgrep/semgrep`.
  - Règles appliquées : `p/java`, `p/owasp-top-ten`, `p/secrets`.
  - Les résultats sont exportés au format SARIF et uploadés dans l'onglet **Security > Code scanning** de GitHub.
  - Le job `build-and-push` dépend désormais de `semgrep` **et** de `test-and-coverage` : un échec Semgrep bloque la publication de l'image Docker.
  - Support optionnel du token `SEMGREP_APP_TOKEN` (secret Github) pour connecter Semgrep Cloud Platform.

- **Épargne – Variation du solde sur une période** : ajout d'un filtre de sélection des comptes dans l'en-tête de la section.
  - Un bouton déroulant "Tous les comptes ▾" permet de cocher/décocher individuellement chaque compte épargne.
  - Une case "Tous les comptes" permet de tout sélectionner ou désélectionner en une action ; elle passe en état indéterminé lors d'une sélection partielle.
  - Le tableau et la ligne **Total** ne tiennent compte que des comptes sélectionnés.
  - Par défaut, tous les comptes sont sélectionnés.
  - Implémentation entièrement côté client (JavaScript), sans modification du contrôleur ni du backend.

## [0.3.10]

### Corrigé

- **Objectifs – dates d'atteinte estimées** : les dates affichées correspondent désormais exactement à l'intersection de chaque courbe avec la ligne objectif.
  - **Tendance** : utilisation de `Math.ceil` au lieu de `Math.round` pour trouver le premier mois entier où la droite de régression linéaire atteint ou dépasse l'objectif.
  - **Projection** : recalcul basé sur le versement mensuel configuré (`ceil(restant / dépôtMensuel)` mois à partir du solde actuel), pour coïncider avec la courbe de projection affichée sur le graphique.

### Modifié

- **Objectifs – affichage** : suppression des boutons radio à côté des dates d'atteinte estimée. Les valeurs Tendance et Projection sont maintenant affichées directement côte à côte sans interaction.


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
