## 1. Préparer l'import par lot

- [x] 1.1 Faire reconnaître et valider les lignes de solde du CSV sans les enregistrer lors d'un nouvel import ; tester que les soldes existants restent inchangés jusqu'au réimport ou à la suppression de leur mois.
- [x] 1.2 Ajouter la validation serveur de 1 à 10 triplets mois/année/CSV et la détection des doublons de période, avec échec des deux lignes concernées et traitement des autres mois.
- [x] 1.3 Orchestrer les imports mois par mois en transactions distinctes, avec résultat ordonné par ligne, message d'erreur sûr et poursuite après échec.

## 2. Interface et sécurité

- [x] 2.1 Ajouter sur la page d'administration des lignes dynamiques mois/année/CSV (bouton `+`, retrait, plafond dix), le bouton de soumission unique et le bilan éphémère par ligne.
- [x] 2.2 Adapter les limites multipart pour dix CSV de 2 Mo maximum et gérer lisiblement le dépassement de taille de requête.
- [x] 2.3 Ajouter une action POST ADMIN avec CSRF et confirmation pour supprimer toutes les lignes d'un mois, sans modifier les autres périodes.

## 3. Vérification et documentation

- [x] 3.1 Tester les lots valides et partiellement réussis, le refus des mois en doublon, les limites serveur, la non-persistance des résultats et le remplacement d'un mois avec anciens soldes.
- [x] 3.2 Tester la suppression d'un mois avec soldes et opérations, le refus des périodes invalides et les contrôles d'accès/CSRF ; vérifier que les anciens JSON restent restaurables sans purge.
- [x] 3.3 Mettre à jour README.md et CHANGELOG.md pour le multi-upload, les résultats indépendants, les limites, l'ignorance des soldes futurs et la suppression manuelle.