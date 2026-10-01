## MODIFIED Requirements

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

## ADDED Requirements

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