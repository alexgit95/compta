## Context

Le Prévisionnel BETA produit en lecture seule des candidats avec libellé indicatif, montant positif, cadence et dates d'occurrence. Pour une série mensuelle comme Navigo, le montant affiché est le total mensuel de plusieurs débits. Les dépenses Budget sont des `RecurringExpense` indépendantes avec libellé libre, montant positif et jour du mois (1-31) ; l'application fournit déjà un formulaire de création avec catégorie facultative et droits ADMIN/EDITOR. Aucun identifiant ne relie les deux sources.

## Goals / Non-Goals

**Goals:**
- Proposer une seule tolérance de montant éditable dans Prévisionnel (4 % par défaut) utilisée par l'analyse et le rapprochement, puis comparer le jour habituel à ±2 jours sans imposer le même libellé.
- Expliquer les rapprochements et distinguer les cas sûrs, partiels, ambigus ou non représentables.
- Proposer la création d'une dépense mensuelle uniquement après ouverture et confirmation d'un formulaire prérempli, sans doublon depuis ce parcours.

**Non-Goals:**
- Enregistrer automatiquement l'analyse, modifier une dépense Budget existante, modéliser une récurrence hebdomadaire dans Budget, ou produire un solde prévisionnel.

## Decisions

### Tolérance commune et cycle de vie

Afficher à côté du bouton d'analyse un contrôle numérique « Tolérance de montant (%) », de 0 à 10 par incréments de 0,5, initialisé à 4. Valider côté serveur avec `BigDecimal` et refuser toute valeur absente, mal formée ou hors plage/pas ; 0 correspond à une égalité de montants. Remplacer la constante de détection par un paramètre transmis à l'analyse, puis passer la même valeur au service de rapprochement : aucune préférence globale ni écriture en base. Après POST, conserver la valeur affichée avec les résultats. Lorsque l'utilisateur ouvre « Ajouter », transporter la période et la tolérance validée pour recalculer côté serveur une candidate fraîche avec la même règle ; ne pas faire confiance aux montants candidats fournis par le navigateur. Alternative écartée : deux champs indépendants pouvant produire un statut Budget calculé avec une autre tolérance que celle qui a permis la détection.

### Total du tableau

Après une analyse avec historique suffisant, additionner une seule fois le montant habituel positif de chaque candidate `MENSUELLE`, y compris celles marquées « Retrouvée », « À vérifier » ou « Non retrouvée ». Les multiples prélèvements déjà réunis dans une candidate (deux Navigo à 90,80 € donnant 181,60 €) ne sont pas additionnés une seconde fois. Afficher en pied de tableau « Total mensuel des charges détectées » avec le montant ; s'il n'y a aucune charge mensuelle, afficher 0 €. Les candidates `HEBDOMADAIRE` restent visibles mais sont explicitement exclues du total, car leur montant est par occurrence et non mensuel. Alternative écartée : additionner des montants de cadences différentes sans conversion, ce qui rendrait le total trompeur.

### Date et montant comparés

Le jour de référence d'un candidat mensuel est la médiane des numéros de jour de ses occurrences sur les trois mois (robuste aux décalages bancaires, y compris deux lignes le même jour). La comparaison des jours est une différence absolue de 2 au maximum ; elle n'exige pas de libellé commun. Comparer `BigDecimal` au montant mensuel habituel du candidat avec la tolérance validée lors de la même analyse. Pour une série de `N` débits par mois, comparer soit une charge Budget égale au total, soit exactement `N` charges Budget proches du montant unitaire (total / `N`) dont la somme correspond au total ; chacune doit respecter la fenêtre de jours. Limiter l'énumération à ces formes et signaler une ambiguïté si plusieurs choix sont plausibles, plutôt que sélectionner arbitrairement une combinaison. Alternative écartée : exiger une similarité de libellé ou additionner toutes les dépenses du même jour, qui confondrait des charges indépendantes.

### États de rapprochement

Calculer le statut à partir des dépenses courantes sans persister de liaison : `RETROUVEE` si une seule correspondance complète montant+jour est plausible, `A_VERIFIER` si les montants sont proches mais le jour diffère, si seules certaines lignes d'une série multiple sont présentes, ou si plusieurs correspondances complètes existent, `NON_RETROUVEE` si aucun montant mensuel ou unitaire pertinent n'est trouvé. Les charges `HEBDOMADAIRE` sont `NON_COMPARABLE`, puisque `RecurringExpense` ne porte qu'un jour mensuel. Afficher, quand disponibles, les dépenses Budget ayant justifié l'état avec montant et jour ; le libellé reste un indice secondaire pour comprendre ou départager visuellement une ambiguïté, pas une condition pour « retrouvée ». Alternative écartée : tout match de montant comme « retrouvée », qui cacherait les erreurs de jour et les correspondances concurrentes.

### Préremplissage et écriture contrôlée

Afficher « Ajouter » seulement pour une candidate mensuelle `NON_RETROUVEE` ou `A_VERIFIER` aux rôles ADMIN/EDITOR ; VIEWER ne voit aucun contrôle de création. Depuis une action liée au candidat de la période analysée, reconstituer le candidat sur le serveur et rejeter une période obsolète ; ne pas faire confiance à un montant ou un jour fourni par le navigateur. Réutiliser la vue de création Budget avec montant mensuel total, jour habituel et libellé indicatif préremplis, catégorie facultative, tous modifiables. La simple ouverture n'écrit rien. À la confirmation, appliquer validation/CSRF et recontrôler l'absence de rapprochement complet ou de doublon du montant/jour dans le Budget, notamment si les dépenses ont changé depuis l'analyse. En cas d'ambiguïté ou de conflit, conserver les données du formulaire avec un message et ne pas enregistrer ; après succès, sauvegarder via le service Budget existant et fournir un retour compréhensible. Alternative écartée : ajout en un clic depuis un résultat BETA pouvant être un faux positif.

Si une série à plusieurs débits est déjà partiellement couverte (un Navigo sur deux), le formulaire rappelle que la valeur préremplie est le total mensuel détecté. Refuser la confirmation de cet agrégat, qui créerait un double comptage, tout en permettant de le corriger vers le montant du seul paiement manquant lorsque le nombre de paiements déjà prévus est inférieur à la multiplicité observée. Une nouvelle confirmation recontrôle la couverture courante.

## Risks / Trade-offs

- [Même montant et même jour peuvent correspondre à plusieurs bénéficiaires] -> Afficher « À vérifier » avec toutes les dépenses plausibles au lieu d'une attribution silencieuse.
- [Une seule dépense Budget de 90,80 € ne couvre pas deux prélèvements Navigo] -> Vérifier le total mensuel et la multiplicité, puis afficher un écart partiel plutôt que « Retrouvée ».
- [Dépense saisie pendant que le formulaire est ouvert] -> Recontrôler les données Budget juste avant l'enregistrement et empêcher le doublon.
- [Tolérance modifiée entre l'analyse et l'ouverture d'un formulaire] -> Revalider le pourcentage transporté et recalculer la candidate côté serveur avant toute sauvegarde.
- [Libellé bancaire variable ou opaque] -> Permettre de modifier le libellé prérempli ; ne jamais le présenter comme identité confirmée.
- [Le Budget ne sait pas stocker les rythmes hebdomadaires] -> État « Non comparable » sans bouton Ajouter, jusqu'à une évolution explicite du modèle.

## Migration Plan

Aucune migration de données ni écriture sur les relevés ; calcul à la demande et ajout uniquement après confirmation de l'utilisateur. Mettre à jour README et CHANGELOG lors de l'implémentation et couvrir par tests les droits, le préremplissage et les rapprochements ambigus.

## Open Questions

Aucune bloquante : les cas sans correspondance unique restent volontairement « À vérifier » et ne déclenchent pas d'ajout automatique.