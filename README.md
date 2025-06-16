# 🛠️ Projet ETL et Business Intelligence avec Apache Spark
Realisé par PAPE ABDOULAYE NDIAYE ET MATY SECK

> **Année académique :** 2024–2025  
> **Université :** Université Gaston Berger (UGB), Institut Polytechnique de Saint Louis (IPSL)  
> **Enseignant :** Dr. Djibril MBOUP  
> **Classe :** GeIT 3

## 📌 Objectif du Projet

Développer une solution complète **ETL (Extract - Transform - Load)** et **Business Intelligence** à partir de la base de données **AdventureWorks** (SQL Server), en exploitant :

- Apache Spark pour le traitement de données
- PostgreSQL comme entrepôt de données
- Power BI pour la visualisation

## ✅ Prérequis techniques

- [Apache Spark 3.5.0](https://spark.apache.org/)
- [Java JDK 8+](https://www.oracle.com/java/technologies/javase-downloads.html)
- [PostgreSQL 14+](https://www.postgresql.org/download/)
- [Power BI Desktop](https://powerbi.microsoft.com/)
- Accès à un conteneur MSSQL avec AdventureWorks (exécuté via Docker dans **une VM Kali sur VirtualBox**)

## ⚙️ Étapes de déploiement

### 1. Connexion à la base MSSQL (hébergée dans la VM Kali)

Assurez-vous que votre VM expose le port 1433 (MSSQL) sur l’hôte.


### 2. Exécution du job Spark ETL a l'aide de la commande "sbt clean assembly"

#### a. Un fichier JAR Spark précompilé sera generé dans le repertoire du code

-
#### b. Lancer l'ETL a l'aide du fichier jar

```bash
spark-submit \
  --class Main \
  --master local[*] \
  --jars "mssql-jdbc-12.10.0.jre11.jar,postgresql-42.7.6.jar" \
  ficher-jar.jar
```

#### c. Paramètres de connexion

- MSSQL : `jdbc:sqlserver://192.168.X.X:1433`
- PostgreSQL : `jdbc:postgresql://localhost:5432/AdventureWorks_DW`

Les identifiants sont à modifier dans le code source (`Main.scala`) si besoin.

---

## 🧠 Modèle de données (PostgreSQL - Schéma en étoile)

- **Tables de dimensions** : `dim_customer`, `dim_product`, `dim_geography`, `dim_date`
- **Table de faits** : `fact_sales`

---

## 📊 Visualisation avec Power BI

### 1. Connexion

- Source : PostgreSQL
- Serveur : `localhost`
- Base de données : `AdventureWorks_DW`

### 2. Tables à importer

```text
dim_customer
dim_product
dim_geography
dim_date
fact_sales
```

### 3. Relations à établir

| Clé           | Vers dimension           |
|---------------|--------------------------|
| CustomerKey   | dim_customer             |
| ProductKey    | dim_product              |
| GeographyKey  | dim_geography            |
| DateKey       | dim_date                 |

---

## 📈 Tableaux de bord disponibles (les tableaux de bords ont etait realisé sur un seul fichier avec des pages differentes)

### 🔹 Ventes

- Chiffre d'affaires total
- Évolution par période (mois, trimestre, année)
- Répartition par catégorie



### 🔹 Clientèle

- Répartition clients par types 
-Par geographie
-Par fidelité en se basant sur les differentes periodes

l'analyse geographique a etait integré aussi bien dans le dashboard Ventes que dans clientèle

### 🔹 Performance

- Taux de croissance,  
- Comparaison des ventes par période.

-
---

## 📁 Structure du dépôt

```
AdventureWorks-ETL-BI/
├── Scala code
│   ├── etl-AdventureWorks
├── etl/
│   ├── etl-master.zip
├── powerbi/
│   ├── DataIng Project.pbix
└── README.md
└── modèle


---
