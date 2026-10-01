## Why

Les relevés importés contiennent désormais l'historique des opérations, mais aucune vue ne permet d'identifier les charges fixes réellement observées. Une première étape du prévisionnel doit rendre ces charges visibles et vérifiables avant de produire des calculs de fin de mois.

## What Changes

- Ajouter un onglet « Prévisionnel (BETA) » à côté d'Administration, accessible aux utilisateurs autorisés à consulter le budget.
- Offrir un bouton « Analyser les charges fixes » qui utilise les trois mois importés consécutifs les plus récents, en excluant toujours le mois en cours ; signaler l'absence d'une telle période.
- Détecter les débits récurrents mensuels ou hebdomadaires à partir des opérations réellement importées, y compris les virements sortants vers l'épargne, puis présenter un tableau des charges candidates avec montant, cadence et occurrences.
- Ne pas enregistrer automatiquement les charges détectées dans le budget manuel et ne pas calculer de solde prévisionnel ni de Holt-Winters dans ce changement.

## Capabilities

### New Capabilities

- `fixed-charge-detection`: analyse BETA à la demande des charges fixes sur une période de trois mois consécutifs importés et consultation des résultats.

### Modified Capabilities

Aucune exigence existante n'est modifiée ; l'historique mensuel et le budget manuel restent inchangés.

## Impact

- Navigation et nouvelle vue Thymeleaf, contrôleur, service Java de sélection d'historique et détection ; lecture seule des relevés JPA existants.
- Contrôle d'accès comparable aux pages de budget, tests du calendrier et des faux positifs, README et CHANGELOG à compléter lors de l'implémentation.
- Aucun schéma de base de données, import CSV, export JSON ni dépendance de machine learning à ajouter.