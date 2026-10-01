## Context

`ForecastController` n'affiche rien au GET et recalcule tous les candidats au POST. `FixedChargeDetectionService.Analysis` contient la période et les candidats, tandis que `BudgetReconciliationService` associe ces candidats aux `RecurringExpense` à partir du Budget actuel. Les imports et suppressions de relevés remplacent/suppriment les lignes d'un mois dans une transaction. L'export/restauration JSON global comprend les relevés mais aucun résultat d'analyse.

## Goals / Non-Goals

**Goals:**
- Charger à l'ouverture le dernier résultat d'analyse enregistré, et le remplacer atomiquement lors d'un nouveau calcul réussi.
- Cesser d'afficher des candidats périmés après un changement pertinent des relevés ou de la fenêtre de trois mois ; permettre de relancer explicitement l'analyse.
- Rendre visibles les dépenses Budget non confirmées par la détection, sans figer les rapprochements dans la sauvegarde de l'analyse.

**Non-Goals:**
- Conserver l'historique de toutes les analyses, recalculer automatiquement à chaque import, changer l'algorithme de détection, modifier le Budget ou produire le solde de fin de mois.

## Decisions

### Résultat sauvegardé

Ajouter un agrégat JPA représentant le seul dernier résultat de l'application : période (`YearMonth` début/fin), tolérance validée, date/heure d'analyse et état valide/invalide, avec ses candidats ordonnés. Chaque candidat conserve libellé, montant habituel `BigDecimal`, cadence, ordre et dates d'occurrence (y compris les doublons de date des prélèvements multiples). Ne pas enregistrer les objets `StatementRow` ni les identifiants ou statuts des dépenses Budget. Un POST d'analyse avec période disponible remplace l'ancien agrégat et ses enfants dans une transaction après calcul réussi ; en cas d'erreur, l'ancien résultat reste intact. Si les trois mois sont insuffisants, signaler l'état existant sans enregistrer de résultat vide présenté comme valide. Alternative écartée : un cache mémoire perdu au redémarrage ou l'enregistrement de l'ensemble des transactions dupliquées dans l'agrégat.

### Chargement et invalidation

Au GET, distinguer : aucun agrégat (invitation à analyser), agrégat valide (candidats et période), agrégat invalide (« Analyse à relancer », sans candidats affichés). La version invalide conserve la trace de la dernière analyse pour distinguer cet état d'une première visite. Dans la transaction d'un import ou d'une suppression de relevé réussi, invalider l'agrégat si sa période contient le mois effectivement modifié ; une validation/import échoué ne change ni les relevés ni l'état de l'analyse. Vérifier aussi au GET que la période sauvegardée correspond encore à la dernière fenêtre de trois mois terminés : si un nouveau mois importé ou le passage au mois suivant change la fenêtre, masquer l'agrégat et indiquer qu'il faut relancer, sans calculer de nouvelles candidates. Alternative écartée : afficher l'ancien résultat en lui attribuant implicitement les nouveaux relevés.

### Rapprochement inverse, recalculé à chaque affichage

Pour une analyse sauvegardée valide, relire `RecurringExpense` et calculer les `Match` avec la tolérance enregistrée. Former une liste de toutes les dépenses Budget non consommées par une correspondance unique `RETROUVEE` : celles présentes parmi les rapprochements `A_VERIFIER` portent « Correspondance incertaine », les autres « Non identifiée dans les charges fixes ». Éviter qu'une même dépense Budget ne soit déclarée confirmée par plusieurs candidates : ces correspondances concurrentes restent « À vérifier ». Afficher libellé, montant, jour et éventuellement raison/lien vers la dépense Budget. Une charge hebdomadaire non comparable ne confirme pas une dépense Budget mensuelle. S'il n'y a aucun résultat valide, ne pas présenter la liste comme une preuve d'absence de charges. Alternative écartée : stocker cette liste dans le snapshot, qui deviendrait périmée après une modification du Budget.

### Compatibilité et droits

Étendre `ExportDto` et la restauration JSON globale à l'agrégat et ses enfants en ordre déterministe. L'absence d'analyse dans une sauvegarde ancienne laisse la base restaurée sans résultat sauvegardé ; la présence d'un résultat restauré est vérifiée contre les relevés avant affichage. Conserver les droits actuels (`ADMIN`/`EDITOR`/`VIEWER` pour consulter/analyser, saisie Budget limitée aux rôles autorisés) et CSRF sur l'analyse. Les liens d'ajout prérempli utilisent uniquement une candidate du résultat valide côté serveur, pas un montant fourni par le client.

## Risks / Trade-offs

- [Un nouvel import ou une restauration rend un résultat obsolète] -> Invalidation transactionnelle des périodes touchées et contrôle de fenêtre au GET ; ne jamais afficher les candidates périmées.
- [Plusieurs dépenses Budget ou plusieurs candidates partagent un montant] -> Correspondance incertaine plutôt qu'affirmer une couverture non unique ; tests de groupes Navigo et d'utilisation unique des dépenses.
- [Le résultat change quand le Budget est édité] -> Recalculer uniquement le rapprochement au GET, sans relancer ni réécrire la détection.
- [Une sauvegarde JSON ancienne ne contient pas de résultat] -> La restaurer sans erreur et afficher l'état « jamais analysé ».

## Migration Plan

Créer la nouvelle table d'analyse et ses occurrences sans changer les relevés existants. Après déploiement, le résultat est absent jusqu'à une première analyse lancée par l'utilisateur. Documenter les règles de mise à jour et les sauvegardes dans README et CHANGELOG lors de l'implémentation.

## Open Questions

Aucune décision fonctionnelle bloquante ; un seul dernier résultat est conservé et les dépenses « À vérifier » sont signalées séparément des absences sans ambiguïté.