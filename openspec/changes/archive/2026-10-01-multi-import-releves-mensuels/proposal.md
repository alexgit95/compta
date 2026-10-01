## Why

L'import actuel oblige à soumettre les relevés un mois après l'autre et ne permet pas de supprimer un mois importé depuis l'interface. Les relevés contiennent en outre des lignes de solde qui n'ont pas vocation à être enregistrées parmi les opérations futures.

## What Changes

- Permettre à l'administrateur d'ajouter ou retirer des lignes mois/année/CSV via un bouton « + », puis de soumettre jusqu'à dix relevés en une seule fois (2 Mo maximum par fichier).
- Afficher après soumission un succès ou une erreur pour chaque ligne ; chaque mois valide est enregistré indépendamment, sans annulation des autres mois en cas d'échec.
- Refuser les deux lignes visant le même mois et la même année dans un lot, tout en traitant les autres mois distincts.
- Ignorer les lignes de solde lors des nouveaux imports CSV : ne conserver que les opérations. Ne pas supprimer automatiquement les lignes de solde déjà stockées ni filtrer celles d'anciennes sauvegardes JSON restaurées.
- Ajouter une suppression manuelle par mois importé, avec confirmation, qui efface toutes ses lignes, opérations et soldes compris, sans toucher aux autres mois.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `monthly-statement-import`: ajouter l'import par lot avec résultats individuels et suppression d'un mois ; ne plus conserver les lignes de solde lors d'un nouvel import CSV.

## Impact

- Formulaire d'administration Thymeleaf/JavaScript, contrôleur d'upload et service transactionnel Spring Boot, limites multipart ; dépôt JPA pour la suppression ciblée.
- Tests du parsing, des lots partiellement réussis, des doublons et de la suppression, plus README et CHANGELOG.
- Aucune migration ou purge automatique des relevés existants ; export/restauration JSON inchangés.