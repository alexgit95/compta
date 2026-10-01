## 1. Sélection des relevés

- [x] 1.1 Ajouter une lecture ciblée des `StatementRow` de type `OPERATION` et une sélection de la dernière séquence de trois mois importés consécutifs avant le mois courant, avec horloge injectable.
- [x] 1.2 Tester les mois manquants, le report vers une séquence plus ancienne, les changements d'année, le mois courant importé et l'état d'historique insuffisant.

## 2. Analyse des charges fixes

- [x] 2.1 Construire des séries de débits par libellé normalisé et montant voisin (tolérance relative de 4 %), sans regrouper des bénéficiaires distincts et en incluant les virements sortants vers l'épargne.
- [x] 2.2 Détecter les séries mensuelles présentes sur les trois mois, y compris les débits multiples au nombre constant par mois, et les séries hebdomadaires espacées de 6 à 8 jours ; produire libellé, médiane des totaux mensuels, cadence et toutes les occurrences.
- [x] 2.3 Tester les séries mensuelles et hebdomadaires, les crédits/soldes exclus, les libellés avec dates variables, les montants identiques de bénéficiaires distincts et les irrégularités.

## 3. Onglet BETA et validation

- [x] 3.1 Ajouter la navigation « Prévisionnel (BETA) » à côté d'Administration et des routes GET/POST protégées comme Budget, avec analyse à la demande et CSRF.
- [x] 3.2 Créer la vue Thymeleaf présentant la période, le tableau des candidats et des états distincts pour historique insuffisant et zéro candidat ; tester l'accès, le lancement et l'absence de modification des charges manuelles.
- [x] 3.3 Mettre à jour README.md et CHANGELOG.md pour le périmètre BETA, les limites de détection et l'absence de projection Holt-Winters dans cette étape ; valider le changement et lancer les tests ciblés.