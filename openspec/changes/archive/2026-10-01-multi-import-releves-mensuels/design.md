## Context

La page `/admin/data` propose aujourd'hui un formulaire unique mois/année/CSV. `StatementCsvReader` accepte un CSV de 2 Mo maximum et crée des lignes `OPERATION` et `BALANCE`, puis `StatementImportService.importMonth` remplace un seul mois sous transaction. La limite HTTP actuelle est de 4 Mo par requête. La capacité `monthly-statement-import` prévoit la conservation des soldes et devra être mise à jour.

## Goals / Non-Goals

**Goals:**
- Soumettre de 1 à 10 mois distincts en une fois avec un résultat visible pour chaque ligne.
- Maintenir la validation, le remplacement et le rollback indépendants par mois, même si un autre fichier du lot échoue.
- Ne plus enregistrer les lignes de solde lors des nouveaux imports et permettre une suppression manuelle complète par mois.

**Non-Goals:**
- Journal permanent des tentatives, purge automatique des soldes historiques, changement du format JSON des sauvegardes, calcul prévisionnel ou prise en charge de nouveaux formats bancaires.

## Decisions

### Formulaire et retour

Sur la page d'administration existante, afficher une première ligne mois/année/CSV, un bouton `+` pour ajouter jusqu'à dix lignes et un contrôle de retrait pour les lignes supplémentaires. Utiliser le JavaScript déjà disponible dans la page pour créer les contrôles avec noms/index cohérents, et garder un bouton de soumission unique. Le serveur valide aussi le nombre de lignes, indépendamment des contrôles navigateur. Après POST, restituer un bilan éphémère dans l'ordre des lignes soumises (mois/année, succès et nombre d'opérations ou erreur explicite), sans stocker les tentatives. Conserver la liste persistante des mois effectivement enregistrés. Alternative écartée : plusieurs soumissions individuelles depuis le navigateur, dont la synthèse commune et la gestion des doublons seraient difficiles.

### Isolation des mois et doublons

L'entrée du contrôleur accepte une liste ordonnée de triplets. Avant de traiter les fichiers, grouper par `YearMonth` pour signaler en échec *toutes* les lignes visant un mois/une année en doublon ; elles ne mutent jamais ce mois. Traiter les autres lignes une à une via le service transactionnel appelé à travers Spring, sans transaction englobant le lot. Attraper les erreurs attendues par ligne afin de poursuivre les suivantes ; ne pas divulguer les messages internes des erreurs de base de données. Conserver `importMonth` et sa transaction pour la compatibilité avec les usages existants. Alternative écartée : une transaction par lot, qui annulerait les succès lorsque le dernier CSV est invalide.

### Soldes, suppression et sauvegarde

Le parseur continue de reconnaître et valider les lignes de solde à quatre ou huit colonnes, mais ne renvoie que les lignes `OPERATION` à huit colonnes destinées au stockage ; le nombre d'opérations doit rester strictement positif. Les anciennes lignes `BALANCE` demeurent en base, visibles, et restent présentes si une ancienne sauvegarde JSON est restaurée. Un réimport d'un mois remplace nécessairement toutes ses anciennes lignes, soldes compris, avec les seules nouvelles opérations. Ajouter une action POST réservée à ADMIN, protégée par CSRF et confirmation dans l'interface, pour supprimer en une transaction toutes les lignes de la période sélectionnée. Refuser une période hors bornes et laisser les autres mois intacts. Alternative écartée : migration supprimant tous les anciens soldes, contraire au choix explicite de nettoyage manuel.

### Limites et tests

Garder la limite fonctionnelle de 2 Mo par fichier et adapter la limite multipart totale à dix fichiers (environ 22 Mo avec marge de formulaire). Conserver la limite serveur par fichier supérieure à 2 Mo pour laisser le lecteur retourner une erreur propre à la ligne ; une requête dépassant la limite globale est rejetée avant le traitement. Tester aussi les limites côté serveur avec une requête fabriquée sans JavaScript, les doublons, les succès partiels, le rollback local, le remplacement des anciens soldes, la suppression complète, les droits et la restitution des résultats.

## Risks / Trade-offs

- [Le conteneur rejette un lot trop volumineux avant le contrôleur] -> Afficher une erreur générale pour cette limite ; documenter les 2 Mo par fichier et la limite globale.
- [Les noms de champs dynamiques peuvent dissocier mois et CSV] -> Lier les champs par index dans un objet de formulaire et vérifier chaque entrée côté serveur.
- [Un échec au milieu d'un lot laisse des mois déjà importés] -> C'est le comportement demandé ; afficher le résultat individuel immédiatement après la soumission.
- [Les sauvegardes anciennes peuvent réintroduire des soldes] -> Ne pas les filtrer implicitement ; conserver la suppression manuelle par mois.

## Migration Plan

Aucune modification de schéma ni purge des données historiques. Déployer le nouveau formulaire et le service de lot ensemble ; les sauvegardes JSON restent compatibles. Mettre à jour README et CHANGELOG lors de l'implémentation.

## Open Questions

Aucune décision fonctionnelle bloquante.