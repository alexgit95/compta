# fixed-charge-detection Specification

## Purpose

Définir l'analyse BETA à la demande des charges fixes à partir de trois mois de relevés consécutifs, sans modifier les charges manuelles du Budget ni produire de projection de solde.

## Requirements

### Requirement: Onglet Prévisionnel BETA
Le système SHALL proposer un onglet « Prévisionnel (BETA) » à côté d'Administration, accessible aux rôles ADMIN, EDITOR et VIEWER. La page SHALL permettre de lancer à la demande une analyse des charges fixes et SHALL afficher le résultat sans enregistrer les candidats dans le Budget.

#### Scenario: Analyse lancée par un utilisateur autorisé
- **WHEN** un utilisateur autorisé ouvre l'onglet et active « Analyser les charges fixes »
- **THEN** le système analyse les opérations disponibles et affiche la période utilisée ainsi que les résultats

#### Scenario: Accès non autorisé
- **WHEN** un utilisateur non authentifié ou dépourvu d'un rôle autorisé tente de consulter ou de lancer l'analyse
- **THEN** l'accès est refusé et aucun résultat n'est exposé

### Requirement: Trois mois consécutifs disponibles
Le système SHALL choisir la séquence de trois mois importés consécutifs la plus récente, strictement antérieure au mois en cours dans le fuseau de l'application. Il SHALL ne pas mélanger des mois séparés par une lacune et SHALL ignorer les mois ne comportant pas d'opérations. Si aucune séquence n'existe, il SHALL afficher un état d'historique insuffisant sans calcul de charges fixes.

#### Scenario: Dernière séquence disponible malgré une lacune récente
- **WHEN** mai, juin, juillet et septembre 2026 sont importés et que l'analyse a lieu en octobre 2026
- **THEN** le système analyse mai à juillet 2026 et n'utilise pas septembre dans cette analyse

#### Scenario: Mois en cours importé
- **WHEN** juillet, août, septembre et octobre 2026 sont importés et que l'analyse a lieu en octobre 2026
- **THEN** le système analyse juillet à septembre 2026 et exclut octobre

#### Scenario: Historique insuffisant
- **WHEN** aucun ensemble de trois mois consécutifs terminés contenant des opérations n'est disponible
- **THEN** la page signale les relevés manquants ou l'insuffisance d'historique au lieu d'afficher une liste de charges

### Requirement: Détection des débits récurrents
Le système SHALL analyser uniquement les lignes `OPERATION` de montant négatif dans la période choisie, y compris les virements sortants vers l'épargne ; les crédits et les anciens soldes SHALL être exclus. Il SHALL d'abord rapprocher les opérations d'après leur libellé normalisé, un écart de montant d'au plus la tolérance choisie pour cette analyse et leur régularité calendaire. Pour les seuls virements non encore rapprochés, il SHALL pouvoir reconnaître un montant et une cadence récurrents malgré des libellés changeants, à condition que plusieurs virements du même montant dans un cycle ne rendent pas ce rapprochement ambigu. Les autres types SHALL rester dépendants du libellé.

#### Scenario: Débits mensuels reconnus
- **WHEN** un même bénéficiaire reçoit un débit de montant voisin environ au même jour lors de chacun des trois mois
- **THEN** une seule charge mensuelle candidate est affichée avec trois occurrences probantes

#### Scenario: Plusieurs débits mensuels identiques
- **WHEN** un même bénéficiaire reçoit deux débits de montant voisin le même jour lors de chacun des trois mois
- **THEN** une seule charge mensuelle candidate est affichée avec six occurrences et un montant habituel égal à la médiane des trois totaux mensuels

#### Scenario: Débit supplémentaire isolé
- **WHEN** le second débit d'un même groupe n'est présent que certains mois
- **THEN** cette multiplicité irrégulière ne constitue pas à elle seule une charge mensuelle candidate

#### Scenario: Débits hebdomadaires reconnus
- **WHEN** un même bénéficiaire reçoit au moins trois débits de montants voisins, répartis sur au moins deux mois et espacés chacun de 6 à 8 jours
- **THEN** une charge hebdomadaire candidate est affichée

#### Scenario: Virement vers l'épargne pris en compte
- **WHEN** un virement sortant vers un livret remplit les critères de régularité
- **THEN** il peut figurer parmi les charges candidates du compte courant

#### Scenario: Libellés variables d'un virement
- **WHEN** un seul virement du même montant revient selon une cadence régulière malgré des libellés différents
- **THEN** une charge candidate affiche le libellé de la première occurrence observée avec la mention « libellé variable », sans présenter ce bénéficiaire comme confirmé

#### Scenario: Virements de même montant ambigus
- **WHEN** plusieurs virements non rapprochés par libellé ont le même montant dans un même cycle
- **THEN** ils ne sont pas fusionnés en une charge candidate par le repli sur le montant

#### Scenario: Opérations non pertinentes ou trompeuses
- **WHEN** des opérations hors virements ont le même montant mais des bénéficiaires différents, sont des crédits ou des soldes, ou ne se répètent pas assez régulièrement
- **THEN** elles ne constituent pas une charge fixe candidate

### Requirement: Tableau explicable et sans effet de bord
Le système SHALL afficher les charges candidates BETA dans un tableau avec libellé, montant habituel positif, cadence, nombre et dates des occurrences, trié par numéro du jour de la première occurrence observée, puis par libellé et montant ; il SHALL afficher sous le tableau le total mensuel des montants des candidates mensuelles, en excluant explicitement les candidates hebdomadaires. Il SHALL afficher un état distinct si aucune charge n'est détectée dans une période pourtant suffisante. L'analyse SHALL ne modifier ni les opérations ni les charges manuelles du Budget et SHALL ne pas calculer de solde prévisionnel.

#### Scenario: Lecture des résultats
- **WHEN** une période de trois mois complète contient des charges détectées
- **THEN** l'utilisateur voit le tableau, les trois mois analysés et les éléments observés justifiant chaque ligne

#### Scenario: Aucune charge candidate
- **WHEN** une période de trois mois complète ne contient aucune série suffisamment régulière
- **THEN** la page indique qu'aucune charge n'a été détectée, sans confondre ce cas avec un manque de relevés

#### Scenario: Indépendance du budget manuel
- **WHEN** l'utilisateur relance l'analyse après avoir importé ou remplacé des relevés
- **THEN** les résultats reflètent les relevés à jour et aucune charge récurrente saisie manuellement n'est créée ou modifiée

#### Scenario: Total mensuel sans double comptage
- **WHEN** le tableau contient une candidate Navigo mensuelle à 181,60 € issue de deux débits, une autre candidate mensuelle à 50 € et une candidate hebdomadaire à 20 €
- **THEN** le pied du tableau affiche 231,60 € de charges mensuelles détectées et signale que la candidate hebdomadaire est exclue de ce total

#### Scenario: Aucune candidate mensuelle
- **WHEN** les seules charges détectées sont hebdomadaires
- **THEN** le pied du tableau indique 0 € de charges mensuelles détectées, hors charges hebdomadaires

### Requirement: Tolérance de montant ajustable
Le système SHALL présenter sur la page Prévisionnel un seul champ « Tolérance de montant (%) » initialisé à 4 %, ajustable de 0 à 10 % par pas de 0,5. Il SHALL valider cette valeur côté serveur avant de lancer l'analyse et utiliser la même tolérance pour la détection et pour le rapprochement Budget du résultat. La valeur choisie SHALL accompagner le parcours d'ajout prérempli afin que la candidate soit recalculée avec la même tolérance avant toute création ; elle SHALL ne pas être enregistrée comme préférence permanente.

#### Scenario: Analyse avec le défaut
- **WHEN** un utilisateur lance l'analyse sans modifier le champ
- **THEN** la détection et le rapprochement utilisent tous deux 4 %

#### Scenario: Analyse avec une tolérance personnalisée
- **WHEN** l'utilisateur saisit 5 % et lance l'analyse
- **THEN** les deux calculs utilisent 5 % et la page montre la valeur effectivement appliquée

#### Scenario: Bornes acceptées
- **WHEN** l'utilisateur saisit 0 % ou 10 % et lance l'analyse
- **THEN** les deux calculs utilisent la valeur sélectionnée sans revenir au défaut de 4 %

#### Scenario: Valeur rejetée
- **WHEN** une requête soumet une valeur négative, supérieure à 10 %, non numérique ou hors du pas de 0,5
- **THEN** aucun calcul n'est lancé avec cette valeur et une erreur de validation est affichée

#### Scenario: Conservation pendant la revue d'une candidate
- **WHEN** une candidate issue d'une analyse à 5 % est ouverte dans le formulaire prérempli
- **THEN** la candidate et son statut Budget sont recalculés avec 5 % avant la confirmation, sans enregistrer ce réglage comme préférence globale

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