# Dispelltacle

## Description du projet

Le projet **Dispelltacle** gère la vente, la réservation et le suivi de billets pour des spectacles.
Il s'inscrit dans le cadre du **BTS SIO SLAM** et couvre la base de données, les services Java et l'interface JavaFX.

Fonctionnalites principales :
- Gestion des utilisateurs
- Affichage des événements à venir
- Achat de billets avec choix des sièges
- Génération et export des billets PDF
- Suivi des ventes et statistiques simples
- Historique des achats
- Nettoyage automatique des événements expirés côté admin

## Base de données

Par défaut, l'application JavaFX cible la base MySQL `dispelltacle`.
Le schéma principal est fourni dans :

```text
Bases_de_Donnees/bdd.sql
```

Sur Windows, le dossier du projet est nommé `Bases_de_Donnees` dans cette documentation, mais il peut apparaître avec l'accent `Bases_de_Données` dans l'explorateur.

La connexion MySQL peut être surchargée avec ces variables d'environnement :

```text
BILLETTERIE_DB_URL
BILLETTERIE_DB_USER
BILLETTERIE_DB_PASSWORD
```

Tu peux aussi les définir dans un fichier `.env` à la racine du projet. Un modèle est fourni dans `.env.example`.

Sans variables, les valeurs utilisees sont :

```text
jdbc:mysql://localhost:3306/dispelltacle?useSSL=false&serverTimezone=UTC
root
mot de passe vide
```

## Données de démonstration

Un jeu de données de démonstration est disponible dans :

```text
Bases_de_Donnees/donnees_exemple.sql
```

Import possible avec XAMPP :

```powershell
C:\xampp\mysql\bin\mysql.exe -u root < Bases_de_Données\bdd.sql
C:\xampp\mysql\bin\mysql.exe -u root dispelltacle < Bases_de_Données\donnees_exemple.sql
```

Les mots de passe de démonstration restent les mêmes pour se connecter (`pass123`, `mdp123`, etc.), mais ils sont stockés en `sha256:` dans les fichiers SQL.

## Jar exécutable

Génération du jar avec dépendances :

```powershell
mvn clean package
```

Le jar autonome est généré ici :

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

## Package runtime Windows

Pour générer une application Windows lançable sans installer JavaFX à part, utilise le script :

```powershell
.\package-runtime.bat
```

Le script lance `mvn clean package`, puis `jpackage`. Le lanceur sera généré ici :

```text
target\package\Dispelltacle\Dispelltacle.exe
```

Note : `mvn javafx:jlink` n'est pas utilisé, car la dépendance ZXing JavaSE est un module automatique et bloque `jlink`.

## Tests

```powershell
mvn test
```

## Règle d'expiration des spectacles

Les spectacles dont la date est passée ne sont plus affichés dans l'espace client.
Lors de l'ouverture du dashboard admin, les événements expirés peuvent être nettoyés avec leurs lignes associées.
