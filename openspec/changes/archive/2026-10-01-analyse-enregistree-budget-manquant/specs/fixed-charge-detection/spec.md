## ADDED Requirements

### Requirement: Dernier résultat d'analyse enregistré
Le système SHALL conserver en base le dernier résultat d'une analyse des charges fixes effectuée avec un historique suffisant : période analysée, tolérance appliquée, date d'analyse, candidats, montants, cadences et toutes leurs dates d'occurrence. À l'ouverture de l'onglet, il SHALL charger ce résultat s'il est valide, sans relancer la détection. Une nouvelle analyse réussie SHALL remplacer atomiquement l'ancien résultat ; une analyse échouée SHALL ne pas écraser un résultat valide.

#### Scenario: Premier accès sans analyse sauvegardée
- **WHEN** l'utilisateur ouvre Prévisionnel avant toute analyse enregistrée
- **THEN** la page propose de lancer l'analyse et ne présente pas de candidats comme déjà calculés

#### Scenario: Retour à l'écran après redémarrage
- **WHEN** un résultat valide comportant deux débits Navigo par mois et une tolérance de 5 % a été enregistré, puis l'utilisateur rouvre Prévisionnel après un redémarrage
- **THEN** les candidats, leurs six occurrences, la période et la tolérance sont restitués de la base sans nouvelle analyse

#### Scenario: Nouvelle analyse remplace le résultat
- **WHEN** l'utilisateur relance avec une tolérance valide différente et que le calcul réussit
- **THEN** la base contient uniquement ce dernier résultat et la page affiche les nouveaux candidats

#### Scenario: Analyse sans historique suffisant ou en erreur
- **WHEN** un résultat valide existe et que la nouvelle analyse ne peut pas aboutir
- **THEN** le résultat précédent n'est pas remplacé par un résultat vide ou partiel et une erreur explicite est présentée

### Requirement: Invalidation des résultats périmés
Le système SHALL invalider le résultat sauvegardé lorsqu'un relevé appartenant à sa période est réimporté ou supprimé avec succès ; un import refusé SHALL ne pas l'invalider. Le système SHALL aussi constater à l'ouverture si la dernière fenêtre de trois mois consécutifs terminés diffère de la période enregistrée, notamment après l'arrivée d'un nouveau mois ou le passage au mois suivant. Il SHALL alors masquer les anciennes candidates et afficher « Analyse à relancer », sans recalcul automatique.

#### Scenario: Réimportation d'un mois analysé
- **WHEN** le relevé d'un des trois mois analysés est remplacé avec succès
- **THEN** le résultat enregistré devient invalide et Prévisionnel indique « Analyse à relancer »

#### Scenario: Suppression d'un mois analysé
- **WHEN** le relevé d'un des trois mois analysés est supprimé
- **THEN** les candidats sauvegardés ne sont plus affichés jusqu'à une nouvelle analyse réussie

#### Scenario: Import échoué ou mois hors période
- **WHEN** un import échoue ou ne modifie qu'un mois hors de la période enregistrée sans changer la fenêtre applicable
- **THEN** le résultat valide reste consultable

#### Scenario: Nouvelle fenêtre de trois mois
- **WHEN** des relevés nouvellement disponibles ou un changement de mois rendent une autre fenêtre de trois mois la plus récente
- **THEN** l'écran indique « Analyse à relancer » et n'attribue pas les anciennes candidates à cette nouvelle période

### Requirement: Sauvegarde du résultat d'analyse
L'export JSON global SHALL inclure le dernier résultat d'analyse enregistré, et sa restauration SHALL rétablir les candidats et occurrences avec les autres données. Une ancienne sauvegarde dépourvue d'analyse SHALL rester restaurable et laisser l'écran sans résultat ; un résultat restauré dont les relevés ne correspondent plus SHALL être masqué comme périmé.

#### Scenario: Cycle de sauvegarde et restauration
- **WHEN** un résultat comprenant des occurrences répétées à la même date est exporté puis restauré avec les relevés correspondants
- **THEN** le résultat, sa tolérance et toutes ses occurrences sont retrouvés sans perte ni recalcul automatique

#### Scenario: Sauvegarde ancienne
- **WHEN** une sauvegarde JSON sans résultat d'analyse est restaurée
- **THEN** la restauration réussit et l'onglet propose une première analyse