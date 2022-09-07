package simpleaggregator;

import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import static org.apache.spark.sql.functions.*;

public class BatchJob
{
    private static final String s3Endpoint = "localhost:9000";

    public static void main(String[] args) throws AnalysisException
    {
        SparkSession spark = SparkSession
                .builder()
                .appName("Simple energy aggregator Spark application")
                .config("spark.ui.port", 12000)
                .config("spark.scheduler.mode", "FAIR")
                .config("spark.sql.shuffle.partitions", 15)
                //.config("spark.sql.streaming.checkpointLocation", checkpointLocation)
                //.config("spark.sql.session.timeZone", "UTC")
                .config("spark.hadoop.fs.s3a.endpoint", s3Endpoint)
                .config("spark.hadoop.fs.s3a.access.key", "MkHLU0kWvwdkoVKz")
                .config("spark.hadoop.fs.s3a.secret.key", "9e6wo9iw2CfpSNO3BeQeAHAfsUYu2aFC")
                .config("spark.hadoop.parquet.enable.summary-metadata", false)
                .config("spark.sql.parquet.mergeSchema", false)
                .config("spark.hadoop.fs.s3a.committer.name", "directory")
                .config("spark.sql.parquet.filterPushdown", true)
                .config("spark.sql.hive.metastorePartitionPruning", true)
                .config("spark.sql.sources.commitProtocolClass", "org.apache.spark.internal.io.cloud.PathOutputCommitProtocol")
                .config("spark.sql.parquet.output.committer.class", "org.apache.spark.internal.io.cloud.BindingParquetOutputCommitter")
                .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
                .config("spark.hadoop.fs.s3a.path.style.access", true)
                //.config("spark.hadoop.fs.s3a.connection.timeout", 10)
                .config("spark.hadoop.fs.s3a.connection.ssl.enabled", false)
                .master("local")
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        StructType messageSchema =  new StructType()
                .add("meterID", DataTypes.LongType, false)
                .add("cityID", DataTypes.LongType, false)
                .add("aggregatedActiveDelta", DataTypes.DoubleType, false)
                .add("date", DataTypes.TimestampType, false)
                .add("year", DataTypes.IntegerType, false)
                .add("month", DataTypes.IntegerType, false)
                .add("hour", DataTypes.IntegerType, false);

        Dataset<Row> batchData = spark
                .read()
                .parquet("s3a://" + "simple-energy-aggregator/energy-data/");
        batchData.printSchema();

        /*System.out.println("Consumption by hours: ");
        batchData
                .withColumn("season",
                        when(col("month").between(1, 3), "Winter")
                                .when(col("month").between(4, 6), "Spring")
                                .when(col("month").between(6, 8), "Summer")
                                .when(col("month").between(8, 12), "Fall")
                )
                .groupBy("hour")
                .pivot("season")
                .avg("aggregatedActiveDelta")
                .sort("hour")
                .show(30, false);*/

        /*System.out.println("Average daily consumption is: ");
        batchData
                .withColumn("day", dayofmonth(col("date")))
                .groupBy("day")
                .avg("day")
                .show();*/

        System.out.println("Top 10 biggest consumers per year are: ");

        batchData.createTempView("data");
        batchData.sqlContext().sql(
                "SELECT t1.meterID, sum(t1.aggregatedActiveDelta) as totalConsumptionInYear, t2.averageYearlyConsumption" +
                " FROM data AS t1" +
                " CROSS JOIN(" +
                        "SELECT avg(totalYearlyConsumption) as averageYearlyConsumption" +
                        " FROM(SELECT year, meterID, sum(aggregatedActiveDelta) as totalYearlyConsumption FROM data GROUP BY year, meterID) AS t2" +
                        " WHERE year = 2022" +
                        " GROUP BY year" +
                        ") AS t2" +
                " WHERE t1.year = 2022" +
                " GROUP BY t1.meterID, t2.averageYearlyConsumption" +
                " HAVING totalConsumptionInYear >= t2.averageYearlyConsumption")
                .show();

        /*batchData
                .groupBy("year", "meterID")
                .sum("aggregatedActiveDelta")
                .sort(desc("sum(aggregatedActiveDelta)"))
                .limit(10)
                .show();*/
    }
}
