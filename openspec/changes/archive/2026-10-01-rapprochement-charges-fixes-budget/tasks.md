## 1. Tolérance de montant commune

- [x] 1.1 Rendre la tolérance paramétrable dans l'analyse des charges fixes et afficher un seul contrôle numérique de 0 à 10 % (pas de 0,5, défaut 4 %) sur la page Prévisionnel.
- [x] 1.2 Valider côté serveur la valeur soumise, la conserver avec les résultats de l'analyse et la transmettre inchangée au rapprochement et au parcours d'ajout, sans préférence persistante.
- [x] 1.3 Tester les analyses à 0, 4, 5 et 10 %, les bornes et les valeurs invalides (dont 0,1 et 10,5 %), y compris les requêtes fabriquées hors de l'IHM.

## 2. Rapprochement montant/jour

- [x] 2.1 Implémenter le rapprochement en lecture seule : jour médian de la candidate, montant mensuel à la tolérance validée, jour Budget à ±2 et correspondances simples ou regroupées pour les séries à plusieurs débits.
- [x] 2.2 Tester les bornes montant/jour avec une tolérance choisie, les libellés différents, une charge Budget agrégée à 181,60 € et deux Navigo à 90,80 €.
- [x] 2.3 Tester les états « À vérifier » (jour éloigné, couverture partielle, groupes ambigus), « Non retrouvée » et « Non comparable » pour les séries hebdomadaires, sans modification des dépenses Budget.
- [x] 2.4 Calculer et tester le total des seules candidates mensuelles, une seule fois par candidate et indépendamment de leur statut Budget ; rendre explicite l'exclusion des candidates hebdomadaires.

## 3. Tableau et ajout confirmé

- [x] 3.1 Ajouter au tableau BETA la colonne de statut Budget, les dépenses rapprochées, le total mensuel en pied de tableau et le bouton « Ajouter » uniquement pour les charges mensuelles admissibles et les rôles ADMIN/EDITOR.
- [x] 3.2 Relier « Ajouter » à la candidate recalculée côté serveur avec la même tolérance et ouvrir le formulaire Budget existant prérempli (montant mensuel, jour, libellé modifiables, catégorie facultative), sans création à l'ouverture.
- [x] 3.3 Mettre en place l'enregistrement explicite et protégé de ce formulaire avec validation, recontrôle des doublons, de la période et de la tolérance avant sauvegarde par BudgetService ; préserver le formulaire et expliquer les conflits.

## 4. Tests et documentation

- [x] 4.1 Tester le flux web complet et ses droits : affichage des quatre statuts et du total mensuel au pourcentage choisi, préremplissage, modifications confirmées, refus VIEWER, CSRF, hebdomadaire et ajout devenu obsolète entre analyse et confirmation.
- [x] 4.2 Mettre à jour README.md et CHANGELOG.md sur le pourcentage commun éditable, le total mensuel hors hebdomadaires, la comparaison montant/jour, les ambiguïtés et l'ajout manuel contrôlé.
- [x] 4.3 Valider le changement OpenSpec et exécuter les tests ciblés du rapprochement et du Budget sans modifier l'import CSV ni le calcul de projection.