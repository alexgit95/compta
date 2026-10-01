## ADDED Requirements

### Requirement: Formulaire d'import mensuel
Le système SHALL proposer à l'administrateur un formulaire de sélection du mois, de l'année et d'un relevé CSV distinct de la restauration JSON globale. Seul un utilisateur ADMIN SHALL pouvoir consulter l'historique et importer un relevé.

#### Scenario: Soumission autorisée
- **WHEN** un administrateur choisit août 2026 et soumet un relevé CSV valide
- **THEN** le système enregistre le relevé d'août 2026 et affiche le nombre d'opérations importées

#### Scenario: Utilisateur sans droit d'administration
- **WHEN** un utilisateur sans rôle ADMIN tente d'accéder au formulaire ou d'importer un relevé
- **THEN** l'accès est refusé et aucune ligne de relevé n'est modifiée

### Requirement: Validation du format du relevé
Le système SHALL accepter le format sans en-tête du CSV fourni : huit champs séparés par point-virgule, date `dd/MM/yyyy`, montant signé en notation française, champs texte facultatifs et lignes de solde distinctes des opérations. Il SHALL valider le fichier entier, sa taille, chaque date et son appartenance au mois et à l'année choisis, chaque montant et la présence d'au moins une opération avant tout remplacement. Il SHALL signaler un fichier invalide sans modifier les données existantes.

#### Scenario: Format du fichier joint
- **WHEN** l'administrateur importe le CSV d'exemple pour août 2026
- **THEN** toutes ses opérations sont acceptées, les montants négatifs et positifs sont préservés et les deux lignes de solde ne sont pas comptées comme dépenses

#### Scenario: Ligne hors période
- **WHEN** un fichier pour août 2026 contient une ligne datée d'un autre mois ou d'une autre année
- **THEN** l'import entier est refusé avec une erreur exploitable et les données précédentes restent intactes

#### Scenario: Fichier vide, mal formé ou sans opération
- **WHEN** le fichier est vide, trop volumineux, comporte un nombre de colonnes incorrect, un montant ou une date invalide, ou uniquement des lignes de solde
- **THEN** l'import entier est refusé et aucune donnée existante n'est supprimée

### Requirement: Historique des opérations
Le système SHALL conserver durablement chaque opération du relevé avec mois et année de rattachement, date, montant signé, informations textuelles et position dans le fichier. Il SHALL préserver les occurrences identiques comme lignes distinctes et distinguer les lignes de solde des opérations. Il SHALL rendre visibles les mois importés et le nombre d'opérations enregistrées par mois.

#### Scenario: Opérations identiques conservées
- **WHEN** un relevé contient deux opérations de même date, montant et libellé
- **THEN** les deux occurrences restent consultables dans le mois importé

#### Scenario: Historique de plusieurs mois
- **WHEN** l'administrateur importe plusieurs mois
- **THEN** chaque mois apparaît avec son nombre d'opérations sans fusionner ses lignes avec celles des autres mois

### Requirement: Remplacement atomique d'un mois
Le système SHALL remplacer uniquement les lignes du mois et de l'année sélectionnés lors d'un nouvel import validé, dans une seule transaction. Les données antérieures SHALL rester inchangées si l'import échoue.

#### Scenario: Réimportation d'un mois
- **WHEN** un relevé valide est importé une seconde fois pour août 2026
- **THEN** les lignes précédemment stockées pour août 2026 sont remplacées sans doublons et celles des autres mois restent intactes

#### Scenario: Échec de la réimportation
- **WHEN** la validation ou l'enregistrement du nouveau relevé échoue
- **THEN** toutes les lignes précédemment stockées pour le mois restent disponibles

### Requirement: Sauvegarde et restauration de l'historique
L'export JSON global SHALL inclure les relevés importés et la restauration JSON globale SHALL les restaurer avec les autres données. Une ancienne sauvegarde sans relevés SHALL rester importable et SHALL donner un historique vide après restauration complète.

#### Scenario: Sauvegarde puis restauration
- **WHEN** un administrateur exporte des données comportant plusieurs relevés puis restaure cet export
- **THEN** tous les mois et toutes les lignes de relevé sont retrouvés sans perte

#### Scenario: Ancienne sauvegarde
- **WHEN** une restauration globale utilise une sauvegarde JSON sans section de relevés
- **THEN** la restauration réussit et l'historique des relevés est vide