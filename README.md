# Billetterie

## Description du projet
Le projet **Billetterie** a pour objectif de gerer la vente, la reservation et le suivi des billets pour des **spectacles**.
Il s'inscrit dans le cadre du **BTS SIO SLAM**, et vise a modeliser une application de gestion complete, depuis la base de donnees jusqu'a l'interface utilisateur.

Fonctionnalites principales :
- Gestion des utilisateurs
- Affichage des evenements a venir uniquement
- Achat de billets avec choix des sieges
- Suivi des ventes et statistiques simples
- Historique des achats
- Nettoyage automatique des evenements expires cote admin

## Base de donnees utilisee
L'application JavaFX cible la base MySQL `billetterie` avec les tables principales :
- `users`
- `tickets`
- `purchases`
- `seats`

## Donnees de demonstration
Un jeu de donnees pour ajouter plusieurs spectacles est fourni dans [Base_de_Donnée/billetterie_seed.sql](C:\xampp\htdocs\billetterie-main\Base_de_Donnée\billetterie_seed.sql).

Import possible avec XAMPP :
```powershell
C:\xampp\mysql\bin\mysql.exe -u root billetterie < Base_de_Donnée\billetterie_seed.sql
```

## Regle d'expiration des spectacles
Les spectacles dont `event_date` est passe ne sont plus affiches dans l'espace client.
Lors de l'ouverture du dashboard admin, les evenements expires sont automatiquement supprimes de `tickets`, avec leurs lignes associees dans `seats` et `purchases`.
