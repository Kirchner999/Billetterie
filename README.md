# Billetterie

## Description du projet

Le projet **Billetterie** gere la vente, la reservation et le suivi de billets pour des spectacles.
Il s'inscrit dans le cadre du **BTS SIO SLAM** et couvre la base de donnees, les services Java et l'interface JavaFX.

Fonctionnalites principales :
- Gestion des utilisateurs
- Affichage des evenements a venir
- Achat de billets avec choix des sieges
- Generation et export des billets PDF
- Suivi des ventes et statistiques simples
- Historique des achats
- Nettoyage automatique des evenements expires cote admin

## Base de donnees

Par defaut, l'application JavaFX cible la base MySQL `dispelltacle`.
Le schema principal est fourni dans :

```text
Bases_de_Donnees/bdd.sql
```

Sur Windows, le dossier du projet est nomme `Bases_de_Donnees` dans cette documentation, mais il peut apparaitre avec l'accent `Bases_de_Données` dans l'explorateur.

La connexion MySQL peut etre surchargee avec ces variables d'environnement :

```text
BILLETTERIE_DB_URL
BILLETTERIE_DB_USER
BILLETTERIE_DB_PASSWORD
```

Sans variables, les valeurs utilisees sont :

```text
jdbc:mysql://localhost:3306/dispelltacle?useSSL=false&serverTimezone=UTC
root
mot de passe vide
```

## Donnees de demonstration

Un jeu de donnees de demonstration est disponible dans :

```text
Bases_de_Donnees/donnees_exemple.sql
```

Import possible avec XAMPP :

```powershell
C:\xampp\mysql\bin\mysql.exe -u root < Bases_de_Données\bdd.sql
C:\xampp\mysql\bin\mysql.exe -u root dispelltacle < Bases_de_Données\donnees_exemple.sql
```

## Jar executable

Generation du jar avec dependances :

```powershell
mvn clean package
```

Le jar autonome est genere ici :

```text
target\billetterie-main-1.0-SNAPSHOT-all.jar
```

Lancement en ligne de commande :

```powershell
java -jar target\billetterie-main-1.0-SNAPSHOT-all.jar
```

Sous Windows, un script de lancement est aussi fourni :

```text
run-billetterie.bat
```

## Tests

```powershell
mvn test
```

## Regle d'expiration des spectacles

Les spectacles dont la date est passee ne sont plus affiches dans l'espace client.
Lors de l'ouverture du dashboard admin, les evenements expires peuvent etre nettoyes avec leurs lignes associees.
