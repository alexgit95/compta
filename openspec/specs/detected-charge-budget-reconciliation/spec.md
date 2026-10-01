# detected-charge-budget-reconciliation Specification

## Purpose

Rapprocher les charges mensuelles détectées avec les dépenses récurrentes du Budget et permettre un ajout contrôlé après vérification dans le formulaire.

## Requirements

### Requirement: Indicateur de rapprochement Budget
Le système SHALL afficher pour chaque charge fixe candidate du tableau BETA un indicateur Budget, calculé à la demande à partir des dépenses récurrentes actuellement configurées. Le rapprochement SHALL donner priorité au montant mensuel (tolérance choisie pour la même analyse, 4 % par défaut) puis au jour habituel (écart maximal de 2 jours pour « Retrouvée ») ; l'égalité des libellés SHALL ne pas être requise. Il SHALL distinguer « Retrouvée », « À vérifier », « Non retrouvée » et « Non comparable » et rendre les dépenses Budget rapprochées visibles pour expliquer la décision.

#### Scenario: Montant et jour uniques malgré des libellés différents
- **WHEN** une charge mensuelle détectée à 100 € autour du 5 est la seule dépense Budget de montant proche et de jour compris entre le 3 et le 7, mais son libellé est différent
- **THEN** elle est marquée « Retrouvée » et la dépense Budget correspondante est identifiable

#### Scenario: Même pourcentage appliqué aux deux calculs
- **WHEN** un utilisateur lance une analyse avec une tolérance valide de 5 %
- **THEN** les rapprochements Budget sont calculés à 5 % sur les candidates détectées à 5 %, sans revenir silencieusement au défaut de 4 %

#### Scenario: Montant correspondant mais jour éloigné
- **WHEN** une dépense Budget de montant proche existe mais son jour diffère de plus de 2 jours du jour habituel détecté
- **THEN** la charge est marquée « À vérifier » avec la dépense Budget plausible

#### Scenario: Plusieurs correspondances possibles
- **WHEN** plusieurs dépenses Budget distinctes satisfont le montant et le jour d'une charge détectée
- **THEN** la charge est marquée « À vérifier » et aucune correspondance unique n'est affirmée

#### Scenario: Aucune correspondance de montant
- **WHEN** aucune dépense Budget de montant mensuel ou unitaire pertinent n'est trouvée
- **THEN** la charge est marquée « Non retrouvée »

#### Scenario: Série hebdomadaire
- **WHEN** une charge candidate est hebdomadaire
- **THEN** elle est marquée « Non comparable », car le Budget actuel ne décrit qu'un jour mensuel

### Requirement: Séries mensuelles à plusieurs débits
Le système SHALL comparer le montant total mensuel habituel d'une série détectée plusieurs fois par mois soit à une seule dépense Budget de même montant, soit à un groupe non ambigu de dépenses Budget correspondant au nombre de débits par mois, proches chacune du montant unitaire, dont la somme et les jours concordent. Une couverture partielle SHALL ne pas être marquée « Retrouvée ».

#### Scenario: Deux Navigo présents dans Budget
- **WHEN** l'analyse détecte deux prélèvements Navigo de 90,80 € par mois autour du 3 et que Budget contient deux dépenses de 90,80 € autour du 3
- **THEN** la charge de 181,60 € mensuels et six occurrences est marquée « Retrouvée » avec les deux dépenses Budget

#### Scenario: Une dépense Budget agrégée
- **WHEN** l'analyse détecte deux prélèvements de 90,80 € par mois autour du 3 et que Budget contient une dépense de 181,60 € autour du 3
- **THEN** la charge est marquée « Retrouvée » avec cette dépense agrégée

#### Scenario: Un seul des deux prélèvements déclaré
- **WHEN** l'analyse détecte deux prélèvements de 90,80 € par mois et que Budget n'en prévoit qu'un de 90,80 €
- **THEN** la charge est marquée « À vérifier » et non « Retrouvée »

### Requirement: Ajout assisté d'une charge détectée
Le système SHALL permettre uniquement aux rôles ADMIN et EDITOR d'ouvrir, depuis une candidate mensuelle « Non retrouvée » ou « À vérifier », le formulaire existant de création d'une dépense récurrente prérempli avec libellé indicatif, montant mensuel total et jour habituel, modifiables avant confirmation ; le rôle VIEWER SHALL ne pas pouvoir utiliser cette action. L'ouverture SHALL ne rien enregistrer. L'enregistrement SHALL exiger une confirmation explicite, une requête protégée par CSRF, la validation de la dépense et un contrôle renouvelé contre les doublons et analyses obsolètes avant de sauvegarder via le service Budget.

#### Scenario: Ouverture du formulaire prérempli
- **WHEN** un utilisateur ADMIN ou EDITOR choisit « Ajouter » sur une charge mensuelle non retrouvée
- **THEN** le formulaire affiche le montant mensuel, le jour et un libellé modifiables avec catégorie facultative, sans créer encore de dépense

#### Scenario: Ajustement avant confirmation
- **WHEN** l'utilisateur corrige le libellé, le montant, le jour ou la catégorie dans le formulaire et confirme
- **THEN** les valeurs validées sont enregistrées comme nouvelle dépense Budget, sans modifier les relevés ni les autres charges

#### Scenario: Ajout d'un paiement manquant dans une série partiellement couverte
- **WHEN** un Navigo sur deux est déjà prévu à 90,80 € et que le formulaire propose le total détecté de 181,60 €
- **THEN** la confirmation du total est refusée pour éviter un double comptage, mais l'utilisateur peut confirmer un second paiement de 90,80 € après avoir ajusté le montant

#### Scenario: Charge déjà couverte ou analyse périmée
- **WHEN** une dépense Budget a été ajoutée entre l'analyse et la confirmation, ou que le candidat n'appartient plus à la période analysée
- **THEN** aucun doublon n'est créé et l'utilisateur reçoit un message lui permettant de revoir la situation

#### Scenario: Accès en lecture seule ou rythme non pris en charge
- **WHEN** un utilisateur VIEWER ou un candidat hebdomadaire consulte le tableau BETA
- **THEN** aucun bouton « Ajouter » utilisable n'est proposé et aucune dépense ne peut être créée par cette action

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