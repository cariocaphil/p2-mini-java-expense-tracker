# P2 DFS BEA Java

Starter code Java du projet P2 OpenClassrooms : application console de suivi des dépenses.

## Contraintes techniques

- Java 25
- Spring Boot 4.0.0, utilisé uniquement pour démarrer l'application console
- Maven
- Persistance locale dans `src/main/resources/emma_expenses.csv`
- Aucun endpoint REST, aucune interface web, aucune base de données et aucun ORM
- Montants représentés avec `BigDecimal`
- Dates représentées avec `LocalDate` au format `dd/MM/yyyy`

## Prérequis

- JDK 25 configuré dans `JAVA_HOME`
- Maven 3.8 ou version ultérieure

## Démarrer le projet

Depuis le dossier `p2-dfs-bea-java/` :

```bash
mvn clean package
mvn spring-boot:run
```

Le fichier CSV est créé avec l'en-tête `date,category,description,amount` s'il est absent ou vide.
L'application accepte les montants saisis avec un point ou une virgule décimale.

Pour lancer le fichier JAR construit :

```bash
java -jar target/p2-dfs-bea-java-0.1.0-SNAPSHOT.jar
```

## Fonctionnalités du starter

- Affichage de l'historique, trié du plus récent au plus ancien
- Ajout d'une dépense dans `emma_expenses.csv`
- Création automatique du fichier et de son en-tête
- Gestion des dates et montants invalides sans stack trace utilisateur
- Gestion d'un historique absent ou vide
- Jeu de données d'exemple dans `src/main/resources/emma_expenses.csv`
- Statistiques par catégorie (option 3 du menu) : total global, nombre de mois couverts et tableau avec le total et la moyenne mensuelle de chaque catégorie. Le nombre de mois est l'étendue calendaire, du mois de la première dépense à celui de la dernière (mois sans dépense inclus) ; chaque moyenne est le total de la catégorie divisé par ce nombre. Les dépenses sans catégorie sont regroupées sous `(uncategorized)`.

- Simulation d'achat d'une voiture (option 4 du menu), voir ci-dessous

## Simulation d'achat d'une voiture

Estime en combien de mois l'utilisateur peut financer une voiture **grâce à son épargne future**.

**Saisies** (demandées à chaque simulation, jamais enregistrées) :

- revenu mensuel net, strictement positif
- prix de la voiture, strictement positif

Les montants acceptent le point ou la virgule décimale. Une saisie invalide affiche un message et ramène au menu.

**Calcul** :

```text
dépenses mensuelles moyennes = total des dépenses / nombre de mois couverts   (arrondi à 2 décimales, HALF_UP)
épargne mensuelle            = revenu mensuel net - dépenses mensuelles moyennes
nombre de mois               = ceil(prix de la voiture / épargne mensuelle)
```

Le total et le nombre de mois couverts sont ceux des statistiques (étendue calendaire entre le mois de la première et celui de la dernière dépense, mois sans dépense inclus).

**Hypothèses** :

- L'épargne de départ est **nulle** : l'argent déjà épargné n'est **pas** pris en compte.
- Le revenu et les dépenses moyennes restent constants chaque mois.
- Si l'épargne mensuelle est nulle ou négative, l'achat ne peut pas être financé par l'épargne : un message l'explique et aucune durée n'est calculée (pas de division par zéro).
- Sans historique de dépenses, les dépenses mensuelles moyennes sont considérées comme **nulles** (épargne = revenu) ; un message le précise dans le résultat.

## Simulation de prêt immobilier (option 5)

Estime le **montant maximum empruntable**, dans la limite d'un taux d'endettement de 35 %.

**Saisies** (jamais enregistrées) : revenu mensuel net (strictement positif), taux d'intérêt annuel en % (0 autorisé), durée en années (entier entre 1 et 50). Le taux est limité à 100 %. Aucune valeur par défaut. Le revenu saisi sert à la fois à l'épargne et au plafond de 35 %.

**Calcul** :

```text
épargne mensuelle  = revenu net - dépenses mensuelles moyennes            (comme la voiture)
mensualité souhaitée = max(épargne, 0) + moyenne mensuelle de la catégorie "housing"
plafond            = 35 % x revenu net
mensualité retenue = min(mensualité souhaitée, plafond)
r = taux annuel / 12 ; n = durée en années x 12
capacité           = mensualité x (1 - (1 + r)^-n) / r      (mensualité x n si le taux est 0)
```

Résultat arrondi à 2 décimales (HALF_UP). Ce n'est pas une formule imposée par le dépôt : elle a été validée pour ce projet (annuité constante + plafond de 35 %).

**Règle métier et limite** :

- Une épargne négative est ramenée à 0 : la mensualité ne descend jamais sous le loyer actuel (le prêt remplace le loyer).
- Simplification : quand les dépenses dépassent le revenu, la capacité peut être **surestimée** ; la console l'indique.
- Sans dépense « housing », le logement vaut 0. Sans historique, les dépenses valent 0.

## Fonctionnalités à implémenter par l'étudiant

- Tests unitaires et tests ciblés des nouvelles fonctionnalités

Les tests préécrits des fonctionnalités avancées ne sont volontairement pas inclus dans ce starter afin que le projet compile avant leur implémentation.

## Organisation du code

```text
src/main/java/com/openclassrooms/p2dfsbea/
├── P2DfsBeaApplication.java  # point d'entrée Spring Boot
├── model/
│   ├── Expense.java
│   ├── ExpenseStatistics.java
│   ├── CategoryStatistics.java
│   ├── CarPurchaseSimulation.java
│   └── MortgageSimulation.java
├── repository/ExpenseRepository.java
├── service/ExpenseService.java
└── ui/ConsoleMenu.java
```

Le projet reste volontairement limité à une application console pédagogique.

