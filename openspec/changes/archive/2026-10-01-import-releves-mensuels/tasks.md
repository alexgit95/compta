## 1. Persistance et parsing

- [x] 1.1 Ajouter l'entité JPA et le dépôt des lignes de relevé avec mois/année, ordre, date, montant signé, champs textuels et type de ligne ; vérifier la compatibilité SQLite et PostgreSQL.
- [x] 1.2 Ajouter Apache Commons CSV et écrire le lecteur du format joint avec vérification stricte des huit colonnes, des dates, des montants, des lignes de solde et des limites de taille.
- [x] 1.3 Tester le lecteur sur le fichier joint et les cas invalides, y compris doublons légitimes, crédits, virements et lignes de solde.

## 2. Import mensuel

- [x] 2.1 Implémenter la validation du mois/année et le remplacement transactionnel des seules lignes du mois ; exposer les mois et nombres d'opérations enregistrées.
- [x] 2.2 Ajouter sur la page d'administration un formulaire CSV distinct du JSON, ses retours de validation et la liste des mois importés, avec accès ADMIN et CSRF.
- [x] 2.3 Tester par intégration les accès, un premier import, la réimportation d'un mois, la conservation des autres mois et l'absence de perte après échec.

## 3. Sauvegarde et documentation

- [x] 3.1 Étendre l'export et la restauration JSON globaux à l'historique des relevés, y compris la compatibilité d'une sauvegarde ancienne sans historique.
- [x] 3.2 Tester par intégration le cycle export/restauration et la restauration d'un ancien JSON.
- [x] 3.3 Documenter le format CSV, le remplacement mensuel et les sauvegardes dans README.md ; alimenter CHANGELOG.md.