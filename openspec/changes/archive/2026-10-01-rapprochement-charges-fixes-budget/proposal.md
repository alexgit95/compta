## Why

La détection BETA montre des charges candidates, mais ne dit pas lesquelles sont déjà prévues dans le Budget. Comparer les montants réellement prélevés aux dépenses récurrentes configurées aidera à repérer un oubli, un montant erroné ou un faux positif avant toute modification.

## What Changes

- Ajouter dans le tableau Prévisionnel (BETA) un indicateur « Budget » : retrouvée, à vérifier, non retrouvée ou non comparable, avec les dépenses Budget retenues pour justifier la comparaison.
- Ajouter sur la page Prévisionnel un pourcentage de tolérance de montant modifiable de 0 à 10 % par pas de 0,5 (4 % par défaut), utilisé à la fois pour l'analyse des charges fixes et leur rapprochement Budget, sans enregistrement d'une préférence permanente.
- Afficher sous le tableau d'analyse le total mensuel des charges candidates mensuelles, sans additionner directement les charges hebdomadaires à ce montant.
- Rapprocher d'abord les montants mensuels avec la tolérance choisie, puis le jour habituel à ±2 jours ; utiliser le libellé uniquement comme indice secondaire. Prendre en compte le montant total d'une série détectée plusieurs fois par mois (par exemple deux Navigo à 90,80 € face à deux dépenses Budget ou une dépense à 181,60 €).
- Pour une charge mensuelle non retrouvée ou à vérifier, proposer aux rôles ADMIN et EDITOR un bouton « Ajouter » ouvrant le formulaire de dépense récurrente prérempli et modifiable ; aucun ajout automatique, vérification antidoublon à la confirmation.
- Ne pas comparer ni proposer l'ajout des charges hebdomadaires dans le Budget actuel, qui ne décrit qu'un jour du mois. Le rôle VIEWER conserve l'accès au tableau sans pouvoir ajouter.

## Capabilities

### New Capabilities

- `detected-charge-budget-reconciliation`: rapprochement explicable des charges mensuelles détectées avec les dépenses récurrentes du Budget et ouverture contrôlée du formulaire d'ajout.

### Modified Capabilities

- `fixed-charge-detection`: remplacer la tolérance fixe de 4 % par un pourcentage validé et ajustable de 0 à 10 % par pas de 0,5, puis afficher le total mensuel des candidates mensuelles.

## Impact

- Services de détection et de rapprochement, page et contrôleur Prévisionnel, formulaire et enregistrement des dépenses Budget, règles d'accès Spring Security.
- Tests du calcul montant/jour, des ambiguïtés, du préremplissage, de l'antidoublon et des permissions ; README et CHANGELOG à compléter lors de l'implémentation.
- Aucun nouveau modèle bancaire, aucune modification du calcul de projection ou de l'import CSV.