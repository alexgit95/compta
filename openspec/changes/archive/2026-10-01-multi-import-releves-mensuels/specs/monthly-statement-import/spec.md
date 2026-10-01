## MODIFIED Requirements

### Requirement: Formulaire d'import mensuel
Le système SHALL proposer à l'administrateur un formulaire distinct de la restauration JSON globale, contenant initialement une ligne mois/année/relevé CSV. Il SHALL permettre d'ajouter des lignes avec un bouton `+`, de retirer les lignes ajoutées et de soumettre de 1 à 10 relevés en une seule fois. Seul un utilisateur ADMIN SHALL pouvoir consulter l'historique, importer un lot et supprimer un mois.

#### Scenario: Soumission autorisée
- **WHEN** un administrateur choisit août 2026 et soumet un relevé CSV valide
- **THEN** le système enregistre le relevé d'août 2026 et affiche le nombre d'opérations importées

#### Scenario: Ajout de plusieurs lignes
- **WHEN** un administrateur clique sur `+` pour ajouter plusieurs triplets mois/année/CSV et soumet le formulaire
- **THEN** le système reçoit tous les relevés choisis dans l'ordre et affiche un résultat pour chacun

#### Scenario: Retrait d'une ligne
- **WHEN** un administrateur retire une ligne avant de soumettre le formulaire
- **THEN** cette ligne n'est pas importée et les autres restent soumissibles

#### Scenario: Utilisateur sans droit d'administration
- **WHEN** un utilisateur sans rôle ADMIN tente d'accéder au formulaire ou d'importer un relevé
- **THEN** l'accès est refusé et aucune ligne de relevé n'est modifiée

### Requirement: Validation du format du relevé
Le système SHALL accepter le format sans en-tête des CSV fournis : huit champs séparés par point-virgule pour les opérations, quatre ou huit pour les lignes de solde, date `dd/MM/yyyy`, montant signé en notation française et champs texte facultatifs. Il SHALL valider chaque fichier entier, sa taille maximale de 2 Mo, chaque date et son appartenance au mois et à l'année choisis, chaque montant et la présence d'au moins une opération avant tout remplacement de ce mois. Les lignes de solde SHALL être reconnues mais SHALL être ignorées à l'enregistrement des nouveaux imports. Un fichier invalide SHALL être signalé sans modifier les données précédentes de son mois.

#### Scenario: Format du fichier joint
- **WHEN** l'administrateur importe le CSV d'exemple pour août 2026
- **THEN** toutes ses opérations sont acceptées, leurs montants négatifs et positifs sont préservés et ses deux lignes de solde ne sont pas enregistrées

#### Scenario: Soldes courts du relevé de juillet
- **WHEN** l'administrateur importe le relevé de juillet 2026 avec des lignes de solde à quatre colonnes et des opérations à huit colonnes
- **THEN** toutes les opérations valides sont enregistrées sans les lignes de solde

#### Scenario: Ligne hors période
- **WHEN** un fichier pour août 2026 contient une ligne datée d'un autre mois ou d'une autre année
- **THEN** l'import de ce mois est refusé avec une erreur exploitable et ses données précédentes restent intactes

#### Scenario: Fichier vide, mal formé ou sans opération
- **WHEN** le fichier est vide, trop volumineux, comporte un nombre de colonnes incorrect, un montant ou une date invalide, ou uniquement des lignes de solde
- **THEN** l'import de ce mois est refusé et ses données existantes ne sont pas supprimées

### Requirement: Historique des opérations
Le système SHALL conserver durablement chaque opération nouvellement importée avec mois et année de rattachement, date, montant signé, informations textuelles et position dans le fichier. Il SHALL préserver les occurrences identiques comme lignes distinctes et rendre visibles les mois importés et leur nombre d'opérations. Les lignes de solde déjà stockées SHALL rester disponibles jusqu'à la suppression manuelle ou au remplacement de leur mois, sans purge automatique ; les anciennes sauvegardes JSON SHALL continuer à être restaurées sans filtrage des soldes.

#### Scenario: Opérations identiques conservées
- **WHEN** un relevé contient deux opérations de même date, montant et libellé
- **THEN** les deux occurrences restent consultables dans le mois importé

#### Scenario: Historique de plusieurs mois
- **WHEN** l'administrateur importe plusieurs mois
- **THEN** chaque mois apparaît avec son nombre d'opérations sans fusionner ses lignes avec celles des autres mois

#### Scenario: Soldes historiques inchangés
- **WHEN** le nouveau formulaire est déployé sur un historique contenant des lignes de solde ou qu'une ancienne sauvegarde contenant des soldes est restaurée
- **THEN** ces lignes restent disponibles et aucune purge automatique n'est effectuée

## ADDED Requirements

### Requirement: Résultats indépendants d'un lot
Le système SHALL traiter chaque mois distinct du lot dans une transaction propre et SHALL afficher après soumission un succès avec le nombre d'opérations ou une erreur par ligne. Un échec SHALL laisser ce mois inchangé et SHALL ne pas annuler les autres imports réussis. Le système SHALL refuser les lots vides ou de plus de dix lignes et SHALL refuser toutes les lignes visant un même mois/année en doublon, sans empêcher le traitement des autres mois distincts. Les résultats des tentatives SHALL ne pas être conservés de manière permanente.

#### Scenario: Lot partiellement réussi
- **WHEN** un lot contient un CSV valide pour août et un CSV invalide pour septembre
- **THEN** août est enregistré, septembre reste inchangé et les deux lignes affichent leurs résultats respectifs

#### Scenario: Même mois indiqué deux fois
- **WHEN** deux lignes d'un lot indiquent août 2026 et une autre indique septembre 2026
- **THEN** les deux lignes d'août sont signalées en échec sans modifier août, et septembre est traité normalement

#### Scenario: Limite de taille du lot
- **WHEN** un lot ne comporte aucune ligne ou dépasse dix lignes, même si la requête est fabriquée sans le formulaire
- **THEN** aucun mois du lot n'est modifié et une erreur générale est affichée

#### Scenario: Résultats non permanents
- **WHEN** un administrateur consulte le bilan d'un lot puis revient plus tard sur l'écran
- **THEN** les tentatives et erreurs du lot ne sont plus affichées, mais les mois effectivement importés restent visibles

### Requirement: Suppression manuelle d'un mois
Le système SHALL offrir à l'administrateur une action de suppression avec confirmation pour chaque mois importé. Après confirmation, l'action SHALL supprimer transactionnellement toutes les lignes de la période visée, opérations et soldes compris, sans affecter les autres périodes ; elle SHALL exiger un accès ADMIN et une protection CSRF.

#### Scenario: Suppression d'un mois contenant des soldes
- **WHEN** un administrateur confirme la suppression d'août 2026 contenant des opérations et des lignes de solde
- **THEN** toutes les lignes d'août sont supprimées et les autres mois restent inchangés

#### Scenario: Suppression refusée
- **WHEN** un utilisateur non ADMIN, une requête sans CSRF ou une période invalide tente de supprimer un mois
- **THEN** aucune ligne de relevé n'est supprimée