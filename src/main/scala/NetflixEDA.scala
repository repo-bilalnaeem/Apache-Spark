import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions._
import org.apache.log4j.{Level, Logger}
import java.nio.file.{Files, Paths, StandardOpenOption}

object NetflixEDA {
  def main(args: Array[String]): Unit = {
    // Suppress excessive logs
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)

    // Initialize Spark Session
    val spark = SparkSession.builder()
      .appName("Netflix EDA")
      .master("local[*]")
      .getOrCreate()

    // Read the Netflix dataset
    val netflixData = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("./netflix_titles.csv")

    // Create output directory
    val outputDir = "./output"
    Files.createDirectories(Paths.get(outputDir))

    // Write schema information to a file
    writeToFile(s"${outputDir}/schema.txt", netflixData.schema.treeString)

    // Sample data output
    saveDataFrame(netflixData.limit(10), s"${outputDir}/sample_data")

    // Count of Movies vs. TV Shows
    val typeCount = netflixData.groupBy("type").count()
    saveDataFrame(typeCount, s"${outputDir}/type_count")

    // Top 10 Countries with the Most Titles
    val topCountries = netflixData.groupBy("country")
      .count()
      .orderBy(desc("count"))
      .limit(10)
    saveDataFrame(topCountries, s"${outputDir}/top_countries")

    // Most Popular Genres
    val popularGenres = netflixData.groupBy("listed_in")
      .count()
      .orderBy(desc("count"))
      .limit(10)
    saveDataFrame(popularGenres, s"${outputDir}/popular_genres")

    // Titles Released Per Year
    val titlesPerYear = netflixData.filter(col("release_year").isNotNull)
      .groupBy("release_year")
      .count()
      .orderBy("release_year")
    saveDataFrame(titlesPerYear, s"${outputDir}/titles_per_year")

    // Null Counts Per Column
    val nullCounts = netflixData.columns.map { colName =>
      val nullCount = netflixData.filter(col(colName).isNull || col(colName) === "").count()
      s"$colName: $nullCount"
    }
    writeToFile(s"${outputDir}/null_counts.txt", nullCounts.mkString("\n"))

    // Stop Spark Session
    spark.stop()
  }

  /**
   * Utility function to save a DataFrame to a CSV file.
   * @param df DataFrame to save
   * @param path Output path
   */
  def saveDataFrame(df: DataFrame, path: String): Unit = {
    df.write
      .mode("overwrite")
      .option("header", "true")
      .csv(path)
  }

  /**
   * Utility function to write text content to a file.
   * @param path File path
   * @param content Content to write
   */
  def writeToFile(path: String, content: String): Unit = {
    Files.write(Paths.get(path), content.getBytes, StandardOpenOption.CREATE)
  }
}
