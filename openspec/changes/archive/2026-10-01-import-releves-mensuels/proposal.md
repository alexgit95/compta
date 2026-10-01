## Why

Les relevés bancaires ne sont actuellement pas enregistrés dans l'application. Sans historique persistant des opérations, il sera impossible de calculer ultérieurement un prévisionnel à partir des dépenses réelles.

## What Changes

- Ajouter un formulaire d'administration pour sélectionner un mois, une année et téléverser un relevé CSV au format fourni.
- Enregistrer durablement les opérations du relevé, avec leurs dates, montants signés et libellés, en distinguant les lignes de solde des transactions.
- Lors d'un nouvel import pour le même mois, remplacer uniquement les données de ce mois, après validation complète du fichier, sans altérer les autres mois.
- Inclure cet historique dans la sauvegarde et la restauration JSON de l'application.
- Ne pas calculer de prévisionnel dans ce changement.

## Capabilities

### New Capabilities

- `monthly-statement-import`: import contrôlé et consultation de l'historique mensuel des opérations bancaires.

### Modified Capabilities

Aucune spécification existante n'est modifiée.

## Impact

- Interface Thymeleaf d'administration, contrôleur et services Spring Boot, entités et dépôt JPA pour l'historique.
- Import/export JSON de sauvegarde et tests unitaires et d'intégration associés.
- Stockage compatible SQLite en local et PostgreSQL en production ; README et CHANGELOG mis à jour lors de l'implémentation.