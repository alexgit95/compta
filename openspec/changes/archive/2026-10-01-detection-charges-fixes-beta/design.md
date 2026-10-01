## Context

L'import CSV stocke les transactions dans `StatementRow` avec date, montant signé, libellés et type `OPERATION`/`BALANCE`. Des anciens soldes peuvent encore exister en base ; les charges mensuelles du Budget sont saisies manuellement dans une autre entité. Le document `docs/previsionnel.md` décrit une détection par montant et intervalles, puis un pipeline Holt-Winters incomplet. Ce changement porte uniquement sur l'étape de détection et son affichage BETA.

## Goals / Non-Goals

**Goals:**
- Analyser sur demande la période de trois mois importés consécutifs la plus récente entièrement antérieure au mois courant.
- Identifier des candidats mensuels/hebdomadaires parmi tous les débits du compte courant, virements vers l'épargne compris, et fournir leurs preuves dans un tableau.
- Rendre les limites de l'analyse visibles sans modifier ou sauvegarder les charges configurées dans Budget.

**Non-Goals:**
- Projeter un solde, calculer Holt-Winters, classer automatiquement les virements internes hors dépenses, générer des charges de Budget ou stocker les résultats de l'analyse.

## Decisions

### Période analysée

Construire des `YearMonth` distincts à partir des lignes de type `OPERATION` déjà importées ; ne considérer que les mois strictement antérieurs au `YearMonth` courant dans le fuseau applicatif. Chercher en remontant la première séquence de trois mois consécutifs présents (pas nécessairement les trois derniers mois calendaires), sans réunir des mois disjoints. Une absence de séquence affiche un état « historique insuffisant » plutôt qu'un tableau vide présenté comme absence de charges. Utiliser une horloge injectable pour tester la limite année/mois et le mois en cours. Alternative écartée : prendre les trois derniers relevés sans vérifier la continuité.

### Classification des charges candidates

Ne sélectionner que les opérations de montant négatif ; convertir leur montant en valeur positive pour l'affichage, mais inclure tous les types de débit, notamment les virements d'épargne. Construire une signature à partir du libellé de débit ou de la référence disponible, normaliser la casse, les espaces, préfixes techniques et dates variables tout en préservant le bénéficiaire ; ne pas regrouper sur le montant seul. Grouper ensuite des montants voisins dans une tolérance relative de 4 % autour d'un montant médian, avec `BigDecimal` pour la monnaie. Alternative écartée : les tranches fixes de 5 euros du document joint, qui ne correspondent pas réellement à 4 % et mêlent des créanciers distincts.

Pour une charge mensuelle, exiger un débit correspondant dans chacun des trois mois, environ au même jour du mois (tolérance de quelques jours). Plusieurs débits du même groupe le même jour sont pris en compte si leur nombre est identique dans chacun des trois mois ; le montant habituel est alors la médiane des trois totaux mensuels, et toutes les dates sont conservées dans les occurrences. Un doublon isolé ne prouve pas une multiplicité récurrente. Pour une charge hebdomadaire, exiger au moins trois occurrences réparties sur au moins deux mois et des intervalles successifs compris entre 6 et 8 jours. Donner priorité à une seule classification par série, puis afficher le libellé, la médiane des montants (totaux mensuels pour les charges mensuelles), la cadence, le nombre et les dates des occurrences ; trier par jour du mois de la première occurrence observée, puis par libellé et montant. Une série insuffisante ou irrégulière n'est pas présentée comme charge fixe. Alternative écartée : retenir seulement la moyenne des intervalles, qui peut masquer des irrégularités.

Lorsque le libellé change pour des opérations de type `Virement`, analyser dans un second passage uniquement les virements non déjà détectés par libellé. Les regrouper par montant voisin (4 %) et cadence, sans produire de candidat si plusieurs virements de même montant se disputent un même mois ou un même jour de cycle. Un candidat issu de ce repli affiche le libellé de l'occurrence la plus ancienne, suivi de « (libellé variable) » : c'est un exemple, pas une identité de bénéficiaire confirmée. Les cartes, prélèvements et autres types restent strictement rapprochés par libellé ; alternative écartée : fusionner toutes les opérations au seul montant, source de faux positifs.

### Interface et sécurité

Ajouter un lien « Prévisionnel (BETA) » à côté d'Administration dans le layout et une page Thymeleaf dédiée, sans page de présentation. Un GET affiche l'écran prêt à analyser ; un POST CSRF sur « Analyser les charges fixes » lit les données et renvoie le tableau ou un état insuffisant. Protéger GET et POST comme le Budget (`ADMIN`, `EDITOR`, `VIEWER`) ; aucun résultat ou paramètre n'est écrit en base. Alternative écartée : réutiliser la projection du Budget existant, dont le modèle est manuel et différent.

## Risks / Trade-offs

- [Trois mois sont peu pour prouver qu'une charge est fixe] -> Afficher « candidats BETA » et les occurrences observées, sans déduire automatiquement de montants du solde.
- [Libellé et montant d'une même charge peuvent varier sensiblement] -> Normaliser conservativement, accepter des faux négatifs et exposer les dates plutôt que fusionner arbitrairement.
- [Deux prélèvements identiques le même mois ou des achats répétés chez un marchand peuvent sembler récurrents] -> Utiliser la signature, la cadence et un représentant par cycle ; tester les cas concrets des relevés importés.
- [Le mois courant ou des lignes de solde historiques biaisent le résultat] -> Exclure explicitement la période courante et filtrer `Kind.OPERATION`.

## Migration Plan

Aucune modification de schéma ni des données importées ; ajouter uniquement la nouvelle page et des requêtes de lecture. Mettre à jour README et CHANGELOG lors de l'implémentation.

## Open Questions

Aucune bloquante pour cette première analyse BETA ; les règles conservatrices seront vérifiées par des tests sur des séries synthétiques et des extraits représentatifs des CSV.