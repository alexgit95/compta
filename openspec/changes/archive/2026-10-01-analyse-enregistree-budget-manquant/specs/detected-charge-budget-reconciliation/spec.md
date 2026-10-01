## ADDED Requirements

### Requirement: Dépenses Budget non confirmées par l'analyse
Lorsqu'un résultat d'analyse sauvegardé est valide, le système SHALL comparer à l'affichage les dépenses récurrentes Budget actuelles aux candidats sauvegardés avec la tolérance enregistrée. Il SHALL présenter sous le tableau une liste des dépenses Budget qui ne participent pas à une correspondance unique « Retrouvée », avec libellé, montant, jour et statut. Les dépenses seulement rapprochées avec l'état « À vérifier » SHALL être signalées comme « Correspondance incertaine » plutôt que déclarées absentes avec certitude ; celles sans rapprochement SHALL être signalées comme « Non identifiée dans les charges fixes ». Il SHALL ne pas persister ces statuts et SHALL ne pas considérer une charge hebdomadaire « Non comparable » comme couverture d'une dépense Budget mensuelle.

#### Scenario: Dépense Budget sans candidate détectée
- **WHEN** une dépense récurrente Budget ne correspond à aucune charge détectée d'une analyse valide
- **THEN** elle apparaît dans la liste inverse comme « Non identifiée dans les charges fixes »

#### Scenario: Dépense Budget déjà confirmée
- **WHEN** une dépense Budget participe à une correspondance unique « Retrouvée »
- **THEN** elle n'apparaît pas parmi les dépenses non confirmées

#### Scenario: Couverture partielle de deux Navigo
- **WHEN** deux débits Navigo mensuels sont détectés et qu'une seule dépense Budget de 90,80 € existe
- **THEN** cette dépense figure avec le statut « Correspondance incertaine » et n'est pas présentée comme entièrement confirmée

#### Scenario: Correspondances concurrentes
- **WHEN** deux candidats pourraient chacun correspondre à la même dépense Budget de montant et jour proches
- **THEN** cette dépense n'est pas consommée deux fois comme couverture confirmée et figure à vérifier

#### Scenario: Modification du Budget après l'analyse
- **WHEN** une dépense Budget est ajoutée, modifiée ou supprimée après l'enregistrement d'une analyse valide
- **THEN** l'indicateur et la liste inverse reflètent le Budget actuel à la prochaine ouverture, sans recalculer ou réécrire les candidats

#### Scenario: Analyse absente ou invalidée
- **WHEN** aucune analyse sauvegardée valide n'est disponible
- **THEN** l'écran ne présente pas les dépenses Budget comme non identifiées par une analyse inexistante ou périmée