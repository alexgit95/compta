## Why

Le Prévisionnel BETA ne conserve pas son analyse : chaque retour à l'onglet masque les résultats, alors que les relevés et la tolérance n'ont pas nécessairement changé. De plus, le rapprochement actuel part des charges détectées et ne révèle pas les dépenses Budget qui ne correspondent à aucune charge fixe observée.

## What Changes

- Conserver en base le dernier résultat d'analyse (période, tolérance, date, candidates et occurrences) ; l'afficher à l'ouverture de Prévisionnel sans relancer la détection, puis le remplacer atomiquement lorsqu'une nouvelle analyse réussit.
- Invalider ce résultat après le remplacement ou la suppression réussie d'un relevé de sa période, ou si la dernière fenêtre de trois mois consécutifs disponibles change ; masquer les candidates périmées et afficher « Analyse à relancer ». Un import échoué ne l'invalide pas.
- Afficher sous le tableau les dépenses récurrentes Budget non couvertes par une correspondance « Retrouvée », en signalant distinctement celles seulement rapprochées avec l'état « À vérifier » ; recalculer cette liste et les indicateurs Budget à partir des dépenses actuelles, sans les stocker dans le résultat d'analyse.
- Préserver le résultat sauvegardé dans l'export/restauration JSON global et gérer les anciennes sauvegardes dépourvues de résultat, sans écrire de nouvelles charges Budget ni modifier l'algorithme de détection.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `fixed-charge-detection`: ajout du dernier résultat persistant, de son affichage à l'ouverture et de son invalidation contrôlée quand les relevés ou la fenêtre d'analyse changent.
- `detected-charge-budget-reconciliation`: ajout d'une liste inverse des dépenses Budget non confirmées par la détection, calculée à l'affichage à partir du Budget courant.

## Impact

- Entités et dépôts JPA pour le résultat et ses occurrences, lecture/écriture de l'analyse, import/suppression des relevés, contrôleur et vue Prévisionnel.
- Extension de l'export/restauration JSON global ; tests d'intégration SQLite/H2 et de sauvegarde, README et CHANGELOG lors de l'implémentation.
- Le calcul des charges candidates et la saisie manuelle dans Budget restent inchangés.