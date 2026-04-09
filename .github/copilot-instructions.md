# Directives technique et cadre de coherence

Mon application doit etre developpee en utilisant les directives techniques et le cadre de coherence suivants :

- Utilisation uniquement de la stack spring boot
Utilisation en local de base de donnees sqlite ,a partie IHM peut etre geré grace à thymleaf ou une autre technologie de template engine compatible avec spring boot
- L'application sera ensuite deployé dans une image docker via portainer et devra utiliser une base de données postgreSQL sur un raspberry pi donc une achitecture ARM64
- L'application doit etre developpee en utilisant le langage de programmation Java
- L'application doit suivre les bonnes pratiques de développement, notamment en termes de structure de code, de gestion des erreurs et de sécurité
- L'application doit être conçue pour être facilement maintenable et évolutive
- Chaque modification doit etre alimenté dans le fichier changelog.md
- Un fichier README.md doit etre créé pour documenter l'application, son installation, son utilisation et sa contribution
- L'application doit être testée de manière approfondie, en utilisant des tests unitaires et d'intégration pour garantir sa fiabilité et sa robustesse, notamment les fonctionnalités lié à l'import export
- Il va aussi etre utiliser les actions de github qui doivent me permettre de construire l'image et de la deployer sur mon docker hub
- Le login et le mot de passe de l'administrateur doivent être stockés dans les variables d'environnement et ne doivent pas être hardcodés dans le code source