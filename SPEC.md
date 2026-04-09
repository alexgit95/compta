Je veux créer une nouvelle application de comptabilité pour la gestion de mon budget et de mon épargne .

Je voudrais un premier onglet dans lequel je puisse saisir la valeur actuelle de mon compte courant et base sur les dépenses récurrente (configurer autre part dans l'appli), projette le montant qu'il me restera à la fin du mois. 
L'affichage peut se faire via un graphique avec une courbe montrant l'évolution de mon compte jusqu'à la fin du mois.

Un second onglet serait l administration lui même divisie en sous partie. Une partie gestion des catégories de dépenses, qui me permet de créer modifier et supprimer des catégories de dépenses ainsi que leur affecter une icône
Une partie dépenses récurrente qui me permet de rentrer les dépenses qui ont lieux au cour d un mois ( montant ,categorie, libellé date dans le mois ou a lieu la dépense), c'est sur cette partie que l'onglet budget doit s'appuyer pour voir les dépenses à venir
Une partie utilisateur qui permet de gérer les utilisateurs (création suppression droits)
Un utilisateur a 3droits possible, administrateur, éditeur, viewer, cette partie , quand je créer un nouveau user un mot de passe doit m être afficher et ce sera son mot de passe.
Une partie import export me permettant de sauvegarder tout le contenu de ma base de données dans un fichier json et la partie import mr permet de tout effacer et dimporter tout le contenu du json.

Tout l'application doit être protéger par un login mot de passe des utilisateurs avec une fonction remember me de 12 mois.
Si possible l'application doit aussi implémenter le passkey pour l'authentification.

Pour finir un endpoint d'export doit être accessible via api key pour récupérer tout l'export (comme via lihm) dans un fichier json
Cette api key doit être générale via lihm d'administration en lui donnant un nom et une durée. Je dois ensuite pouvoir voir toutes les api key déclaré, leur date d'expiration et leur date de dernière utilisation

Un troisième onglet serait l'onglet d'épargne. Dans celui ci je dois pouvoir créer un nouveau compte, donné sa valeur a une date donné et combien est versé chaque mois.( Je veux a tout moment pouvoir éditer ce compte en modifiant le libellé et le montant chaque mois)
Je dois pouvoir voir tous les comptes épargnes déclaré et la simulation de leur valeur ( pour cela je prend le montant qui a été indiqué a une date donné et j'y ajoute les sommes qui ont été versé dessus depuis en multipliant le nombre de mois par le montant versé chaque mois)
Je dois a tout moment pouvoir redonner a la date courant le montant sur ce compte pour actualiser la valeur du compte et ses projections.
Dans la partie épargne je veux des graphiques avec l'évolution de mon épargne en faisant apparaitre pour chaque compte un point montrant une valeur entrée a une date et bien défini, et à chaque fois la courbe de projection entre les points et jusqu'à aujourd'hui 

L'onglet administration ne doit être visible que par l'administrateur. Un viewer ne peut rien modifier dans l'appli et un éditeur ne peut qu'utiliser la partie budget et entrer de valeurs du solde du compte pour la partie epargne
