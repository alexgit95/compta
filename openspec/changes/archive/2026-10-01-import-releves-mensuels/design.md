## Context

L'application Spring Boot / Thymeleaf stocke des charges récurrentes saisies à la main, mais aucune transaction issue de relevé. L'écran `/admin/data` traite uniquement des sauvegardes JSON qui remplacent toute la base. Le fichier d'exemple est un CSV sans en-tête à huit colonnes séparées par `;` : date `dd/MM/yyyy`, montant signé à virgule décimale, type et champs de libellé variables. La première et la dernière ligne représentent des soldes, non des opérations. SQLite est utilisé en local et PostgreSQL en production.

## Goals / Non-Goals

**Goals:**
- Permettre à l'administrateur d'importer un relevé pour un mois/une année choisis et de voir les mois importés et leur nombre d'opérations.
- Préserver les lignes bancaires, y compris crédits et virements, avec leur montant signé et leur ordre ; distinguer les lignes de solde des opérations.
- Remplacer atomiquement les seules données du mois sélectionné ; intégrer cet historique aux sauvegardes JSON.

**Non-Goals:**
- Classification automatique des dépenses, détection de récurrence, Holt-Winters, écran de prévision, rapprochement bancaire ou prise en charge de tous les formats CSV bancaires.

## Decisions

### Emplacement et accès

Ajouter une section dédiée aux relevés sur `/admin/data` avec sélecteurs mois/année, fichier CSV, retour d'erreurs et liste des mois importés. Réserver consultation et import au rôle ADMIN, dans la continuité de la page existante ; préserver la protection CSRF et séparer explicitement l'import CSV de la restauration JSON globale. Alternative écartée : créer dès maintenant un onglet Prévisionnel vide et accessible à tous.

### Modèle de données

Créer une entité JPA de ligne de relevé avec mois/année de rattachement, numéro de ligne, date, montant signé `BigDecimal`, type, libellés bruts utiles et indicateur `OPERATION` / `BALANCE`. Conserver les opérations positives et négatives sans les agréger ni supprimer les virements internes ; le futur prévisionnel décidera de leur traitement. Conserver aussi les lignes de solde identifiées, pour permettre audit et contrôle, sans les compter parmi les dépenses. Identifier chaque ligne par son mois/année et sa position, afin de préserver les opérations identiques le même jour. Alternative écartée : ne garder que les montants négatifs ou le CSV brut sans modèle requêtable.

### Lecture et remplacement

Utiliser un parseur CSV éprouvé (Apache Commons CSV) pour les champs délimités/éventuellement quotés, puis parser les dates `dd/MM/yyyy` strictement et les montants français en `BigDecimal`. Accepter un éventuel BOM UTF-8, exactement huit colonnes et les lignes sans en-tête du modèle fourni. Identifier les lignes de solde par leur structure (absence de type et de libellé d'opération, référence bancaire seule), pas seulement par le signe ou la position ; rejeter les lignes inconnues plutôt que les ignorer silencieusement. Valider fichier non vide, taille limitée, dates et mois/année de chaque ligne, montants et structure avant toute mutation. Refuser un relevé sans opération. Le service transactionnel supprime les lignes de ce mois puis insère les nouvelles ; toute erreur conserve le relevé précédent. Les autres mois restent inchangés. Alternative écartée : suppression au début du flux de lecture ou dédoublonnage par date et montant.

### Sauvegardes et tests

Étendre `ExportDto` et la restauration JSON pour inclure les lignes historiques ; lors d'une restauration complète, les relever comme les autres données, y compris si une ancienne sauvegarde ne contient pas cette section (historique alors vide). Tester la lecture du CSV d'exemple, les lignes de solde, les libellés différents selon le sens, les montants français, les doublons légitimes, un mois erroné, un remplacement valide/invalide, la sécurité et le round-trip JSON. Ne pas conserver le fichier uploadé sur disque.

## Risks / Trade-offs

- [Le format peut varier selon la banque ou l'encodage] -> Documenter la version prise en charge, donner une erreur explicite au lieu d'importer partiellement ; élargir le format dans un changement futur.
- [La restauration d'un ancien JSON est destructive par contrat] -> Documenter que l'absence d'historique efface celui déjà présent et préserver la confirmation actuelle de restauration complète.
- [Des lignes identiques peuvent être de vraies transactions distinctes] -> Garder la position de ligne plutôt que dédupliquer automatiquement.
- [Données bancaires sensibles et fichiers volumineux] -> Rôle ADMIN, validation de taille, pas de journalisation des lignes et pas de stockage du fichier original.

## Migration Plan

Ajouter la table JPA sans modifier les données existantes ; permettre les relevés historiques mois par mois. Mettre à jour README et CHANGELOG lors de l'implémentation. Avant déploiement, effectuer une sauvegarde de la base ; en cas de retour arrière, conserver la table historique (la version précédente l'ignore) et restaurer la sauvegarde si nécessaire.

## Open Questions

Aucune bloquante pour ce premier lot ; le périmètre se limite au format CSV joint et aux utilisateurs ADMIN.