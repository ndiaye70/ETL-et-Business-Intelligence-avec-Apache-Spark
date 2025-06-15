import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions._
import java.util.Properties

object Main {
  def main(args: Array[String]): Unit = {
    // 1. Initialisation de la session Spark
    val spark = SparkSession.builder()
      .appName("AdventureWorks ETL")
      .config("spark.sql.shuffle.partitions", "8")
      .getOrCreate()

    // 2. Connexion MSSQL
    val jdbcUrlMssql = "jdbc:sqlserver://192.168.31.83:1433;databaseName=AdventureWorks;encrypt=true;trustServerCertificate=true"
    val mssqlProps = new Properties()
    mssqlProps.setProperty("user", "sa")
    mssqlProps.setProperty("password", "password123?")

    // 3. Extraction des données depuis MSSQL
    val df_customer = spark.read.jdbc(jdbcUrlMssql, "Sales.Customer", mssqlProps)
      .select("CustomerID", "PersonID", "StoreID")

    val df_person = spark.read.jdbc(jdbcUrlMssql, "Person.Person", mssqlProps)
      .select("BusinessEntityID", "FirstName", "LastName", "EmailPromotion")

    val df_product = spark.read.jdbc(jdbcUrlMssql, "Production.Product", mssqlProps)
      .select("ProductID", "Name", "ProductNumber", "ProductSubcategoryID")

    val df_product_subcategory = spark.read.jdbc(jdbcUrlMssql, "Production.ProductSubcategory", mssqlProps)
      .select("ProductSubcategoryID", "ProductCategoryID", "Name")

    val df_product_category = spark.read.jdbc(jdbcUrlMssql, "Production.ProductCategory", mssqlProps)
      .select("ProductCategoryID", "Name")

    val df_sales_header = spark.read.jdbc(jdbcUrlMssql, "Sales.SalesOrderHeader", mssqlProps)
      .select("SalesOrderID", "CustomerID", "OrderDate", "ShipToAddressID")

    val df_sales_detail = spark.read.jdbc(jdbcUrlMssql, "Sales.SalesOrderDetail", mssqlProps)
      .select("SalesOrderID", "SalesOrderDetailID", "ProductID", "OrderQty", "UnitPrice", "LineTotal")

    val df_address = spark.read.jdbc(jdbcUrlMssql, "Person.Address", mssqlProps)
      .select("AddressID", "City", "PostalCode", "StateProvinceID")

    val df_emailAddress = spark.read.jdbc(jdbcUrlMssql, "Person.EmailAddress", mssqlProps)
      .select("BusinessEntityID", "EmailAddress")


    val df_state = spark.read.jdbc(jdbcUrlMssql, "Person.StateProvince", mssqlProps)
      .select("StateProvinceID", "Name", "CountryRegionCode")

    val df_region = spark.read.jdbc(jdbcUrlMssql, "Person.CountryRegion", mssqlProps)
      .select("Name", "CountryRegionCode")

    // 4. Transformations

    val dimCustomer = df_customer
      .join(df_person, df_customer("PersonID") === df_person("BusinessEntityID"), "left")
      .join(df_emailAddress, df_person("BusinessEntityID") === df_emailAddress("BusinessEntityID"), "left")
      .withColumn("CustomerType", when(col("StoreID").isNotNull, "Bussiness").otherwise("Individual"))
      .select(
        df_customer("CustomerID").alias("CustomerKey"),
        df_person("FirstName"),
        df_person("LastName"),
        df_emailAddress("EmailAddress"),
        col("CustomerType")
      )
      .na.fill(Map("FirstName" -> "Inconnu", "LastName" -> "Inconnu", "EmailAddress" -> "Null"))

    val dimProduct = df_product
      .join(df_product_subcategory, df_product("ProductSubcategoryID") === df_product_subcategory("ProductSubcategoryID"), "left")
      .join(df_product_category, df_product_subcategory("ProductCategoryID") === df_product_category("ProductCategoryID"), "left")
      .select(
        df_product("ProductID").alias("ProductKey"),
        df_product("Name").alias("ProductName"),
        df_product("ProductNumber"),
        df_product_category("Name").alias("ProductCategory"),
        df_product_subcategory("Name").alias("ProductSubcategory")
      )
      .na.fill(Map("ProductCategory" -> "Non catégorisé", "ProductSubcategory" -> "Non catégorisé"))

    val dimGeography = df_address
      .join(df_state, df_address("StateProvinceID") === df_state("StateProvinceID"), "left")
      .join(df_region,df_state("CountryRegionCode") === df_region("CountryRegionCode"),"left")
      .select(
        df_address("AddressID").alias("GeographyKey"),
        df_address("City"),
        df_state("Name").alias("StateProvince"),
        df_region("Name").alias("CountryRegion"),
          df_address("PostalCode")
      )

    val dimDate = spark.sql(
      """
           SELECT
      CAST(date_format(date, 'yyyyMMdd') AS INT) AS DateKey,
      date AS FullDate,
      YEAR(date) AS Year,
      MONTH(date) AS Month,
      date_format(date, 'MMMM') AS MonthName,
      CAST(QUARTER(date) AS INT) AS Quarter
    FROM (
      SELECT explode(sequence(to_date('2011-01-01'), to_date('2014-12-31'), interval 1 day)) AS date
    )

      """)

    val factSales = df_sales_detail
      .join(df_sales_header, "SalesOrderID")
      .join(dimCustomer, df_sales_header("CustomerID") === dimCustomer("CustomerKey"))
      .join(dimProduct, df_sales_detail("ProductID") === dimProduct("ProductKey"))
      .join(dimGeography, df_sales_header("ShipToAddressID") === dimGeography("GeographyKey"))
      .select(
        dimCustomer("CustomerKey"),
        dimProduct("ProductKey"),
        dimGeography("GeographyKey"),
        date_format(df_sales_header("OrderDate"), "yyyyMMdd").cast("int").alias("DateKey"),
        df_sales_detail("OrderQty").alias("OrderQuantity"),
        df_sales_detail("LineTotal").alias("SalesAmount"),
        df_sales_detail("UnitPrice")
      )

    // 5. Chargement vers PostgreSQL
    val jdbcUrlPostgres = "jdbc:postgresql://localhost:5432/AdventureWorks_DW?rewriteBatchedStatements=true"
    val postgresProps = new Properties()
    postgresProps.setProperty("user", "postgres")
    postgresProps.setProperty("password", "teheran07")
    postgresProps.setProperty("batchsize", "10000")

    dimCustomer.repartition(8).write.mode("overwrite").jdbc(jdbcUrlPostgres, "dim_customer", postgresProps)
    dimProduct.repartition(8).write.mode("overwrite").jdbc(jdbcUrlPostgres, "dim_product", postgresProps)
    dimGeography.repartition(8).write.mode("overwrite").jdbc(jdbcUrlPostgres, "dim_geography", postgresProps)
    dimDate.repartition(8).write.mode("overwrite").jdbc(jdbcUrlPostgres, "dim_date", postgresProps)
    factSales.repartition(16).write.mode("overwrite").jdbc(jdbcUrlPostgres, "fact_sales", postgresProps)

    // 6. Validation
    println("Données chargées avec succès dans PostgreSQL !")
    println(s"Nombre de clients chargés: ${dimCustomer.count()}")
    println(s"Nombre de produits chargés: ${dimProduct.count()}")
    println(s"Nombre de ventes chargées: ${factSales.count()}")

    spark.stop()
  }
}
