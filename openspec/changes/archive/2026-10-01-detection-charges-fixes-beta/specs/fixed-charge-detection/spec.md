## ADDED Requirements

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
Le système SHALL analyser uniquement les lignes `OPERATION` de montant négatif dans la période choisie, y compris les virements sortants vers l'épargne ; les crédits et les anciens soldes SHALL être exclus. Il SHALL d'abord rapprocher les opérations d'après leur libellé normalisé, un écart de montant d'au plus 4 % et leur régularité calendaire. Pour les seuls virements non encore rapprochés, il SHALL pouvoir reconnaître un montant et une cadence récurrents malgré des libellés changeants, à condition que plusieurs virements du même montant dans un cycle ne rendent pas ce rapprochement ambigu. Les autres types SHALL rester dépendants du libellé.

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
Le système SHALL afficher les charges candidates BETA dans un tableau avec libellé, montant habituel positif, cadence, nombre et dates des occurrences, trié par numéro du jour de la première occurrence observée, puis par libellé et montant ; il SHALL afficher un état distinct si aucune charge n'est détectée dans une période pourtant suffisante. L'analyse SHALL ne modifier ni les opérations ni les charges manuelles du Budget et SHALL ne pas calculer de solde prévisionnel.

#### Scenario: Lecture des résultats
- **WHEN** une période de trois mois complète contient des charges détectées
- **THEN** l'utilisateur voit le tableau, les trois mois analysés et les éléments observés justifiant chaque ligne

#### Scenario: Aucune charge candidate
- **WHEN** une période de trois mois complète ne contient aucune série suffisamment régulière
- **THEN** la page indique qu'aucune charge n'a été détectée, sans confondre ce cas avec un manque de relevés

#### Scenario: Indépendance du budget manuel
- **WHEN** l'utilisateur relance l'analyse après avoir importé ou remplacé des relevés
- **THEN** les résultats reflètent les relevés à jour et aucune charge récurrente saisie manuellement n'est créée ou modifiée