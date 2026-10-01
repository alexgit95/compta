## 1. Résultat persistant

- [x] 1.1 Créer le modèle et le dépôt JPA du seul dernier résultat d'analyse, de ses candidats ordonnés et de toutes leurs dates d'occurrence, y compris les dates dupliquées et la tolérance appliquée.
- [x] 1.2 Enregistrer atomiquement le résultat d'une analyse réussie et charger au GET les états « jamais analysé », « résultat valide » et « analyse à relancer », sans lancer de détection au GET.
- [x] 1.3 Tester la conservation après redémarrage, le remplacement du résultat avec une autre tolérance, l'absence de perte après erreur et les deux Navigo répétés par mois.

## 2. Invalidation des relevés

- [x] 2.1 Invalider dans la même transaction le résultat lorsque l'import ou la suppression réussie touche un mois analysé ; conserver le résultat sur un import refusé ou un mois hors période.
- [x] 2.2 Comparer au GET la période sauvegardée à la dernière fenêtre disponible et masquer les candidats si un nouveau relevé ou un changement de mois rend cette période obsolète.
- [x] 2.3 Tester le réimport et la suppression ciblés, un échec d'import, un nouveau mois, un passage d'année/mois et le retour visible de « Analyse à relancer ».

## 3. Liste inverse du Budget

- [x] 3.1 Dériver à l'affichage la liste des dépenses Budget non couvertes par une correspondance unique « Retrouvée » à partir des rapprochements existants ; distinguer « Correspondance incertaine » de « Non identifiée » et détecter les couvertures concurrentes.
- [x] 3.2 Ajouter sous le tableau la liste des dépenses Budget non confirmées avec leur libellé, montant, jour et statut, sans afficher cette liste comme résultat d'analyse lorsque le snapshot est absent ou invalide.
- [x] 3.3 Tester les dépenses confirmées, partielles (Navigo), concurrentes, hebdomadaires non comparables et le rafraîchissement immédiat après ajout/modification/suppression Budget sans relancer l'analyse.

## 4. Sauvegarde et documentation

- [x] 4.1 Ajouter le dernier résultat à l'export/restauration JSON global en respectant les liens JPA et le remplacement global ; une ancienne sauvegarde sans analyse donne l'état « jamais analysé ».
- [x] 4.2 Tester le cycle JSON avec tolérance et occurrences dupliquées, la restauration d'une sauvegarde ancienne, et le masquage d'un résultat restauré devenu incompatible avec les relevés.
- [x] 4.3 Mettre à jour README.md et CHANGELOG.md ; valider OpenSpec, lancer les tests ciblés et vérifier le démarrage avec SQLite local.