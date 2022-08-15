package simpleaggregator;

import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.OutputMode;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import static org.apache.spark.sql.functions.*;

import java.util.concurrent.TimeoutException;

public class App
{
    private static final String hourlyConsumerTopic = "hourlyConsumptionByConsumer";
    private static final String totalByCityTopic = "totalByCity";
    private static final String totalConsumptionTopic = "totalConsumption";

    private static final int countryWindowDurationSeconds = 3;
    private static final int totalByCityWindowDurationSeconds = 3;

    private static final String checkpointRootLocation = "s3a://simple-energy-aggregator/aggregator-checkpoints/"; // "C:\\Users\\Korisnik\\Desktop\\Spark checkpoint\\";
    private static final String cityAggregationsCheckpointLocation = checkpointRootLocation + "city-aggregations-checkpoints";
    private static final String countryAggregationsCheckpointLocation = checkpointRootLocation + "country-aggregations-checkpoints";
    private static final String hourlyConsumerAggregationsCheckpointLocation = checkpointRootLocation + "hourlyConsumer-aggregations-checkpoints";

    private static final String kafkaBootstrapServers = "desktop-kafka-1:9092";//"127.0.0.1:9092";
    private static final String inputTopicName = "input";
    private static final String cityCoordinatesCsvPath = "s3a://simple-energy-aggregator/city-coordinates.csv";

    public static void main(String[] args) throws TimeoutException, StreamingQueryException
    {
        // https://spark.apache.org/docs/latest/submitting-applications.html#master-urls
        // https://databricks.com/blog/2017/04/26/processing-data-in-apache-kafka-with-structured-streaming-in-apache-spark-2-2.html
        // https://stackoverflow.com/questions/37132559/add-jar-files-to-a-spark-job-spark-submit

        // spark-submit.cmd --master local --deploy-mode client --class simpleaggregator.App --name Testiranje --packages org.apache.spark:spark-sql-kafka-0-10_2.13:3.3.0,org.apache.spark:spark-hadoop-cloud_2.13:3.3.0 "D:\Diplomski rad\Prakticni dio\Simple energy aggregator\target\simple-energy-aggregator-1.0-SNAPSHOT.jar"

        // setup
        SparkSession spark = SparkSession
                .builder()
                .appName("Simple energy aggregator Spark application")
                //.config("spark.ui.port", 12000)
                //.config("spark.scheduler.mode", "FAIR")
                .config("spark.sql.shuffle.partitions", 20)
                //.config("spark.sql.streaming.checkpointLocation", checkpointRootLocation)
                //.config("spark.sql.session.timeZone", "UTC")
                //.config("spark.hadoop.fs.s3a.endpoint", "localhost:9000")
                .config("spark.hadoop.fs.s3a.endpoint", "minio:9000")
                //.config("spark.hadoop.fs.s3a.access.key", "MkHLU0kWvwdkoVKz")
                //.config("spark.hadoop.fs.s3a.secret.key", "9e6wo9iw2CfpSNO3BeQeAHAfsUYu2aFC")
                .config("spark.hadoop.fs.s3a.committer.name", "directory")
                .config("spark.sql.sources.commitProtocolClass", "org.apache.spark.internal.io.cloud.PathOutputCommitProtocol")
                .config("spark.hadoop.fs.s3a.path.style.access", true)
                //.config("spark.hadoop.fs.s3a.connection.timeout", 10000)
                .config("spark.hadoop.fs.s3a.connection.ssl.enabled", false)
                //.master("local")
                .getOrCreate();
        spark.sparkContext().setLogLevel("WARN");
        System.out.println("spark.hadoop s3a endpoint is: " + spark.conf().get("spark.hadoop.fs.s3a.endpoint"));
        System.out.println("hadoop s3a endpoint is: " + spark.sparkContext().hadoopConfiguration().get("fs.s3a.endpoint"));
        System.out.println("CSV file path is: " + cityCoordinatesCsvPath);


        // the purpose of spark.hadoop.fs.s3a.path.style.access is explained at https://medium.com/@e.osama85/the-difference-between-the-amazon-s3-path-style-urls-and-virtual-hosted-style-urls-4fafd5eca4db
        // and https://www.redhat.com/en/blog/anatomy-s3a-filesystem-client

        StructType messageSchema =  new StructType()
                .add("meterID", DataTypes.LongType, false)
                .add("cityID", DataTypes.LongType, false)
                .add("timestamp", DataTypes.LongType, false)
                .add("activeDelta", DataTypes.DoubleType, false);

        StructType cityCoordinatesSchema = new StructType()
                .add("cityID", DataTypes.LongType, false)
                .add("latitude", DataTypes.DoubleType, false)
                .add("longitude", DataTypes.DoubleType, false)
                .add("cityName", DataTypes.StringType, false);

        // CSV file has a header
        Dataset<Row> cityCoordinates = spark
                .read()
                .option("header", true)
                .schema(cityCoordinatesSchema)
                .csv(cityCoordinatesCsvPath);
        cityCoordinates.show();

        //messageSchema.printTreeString();

        Dataset<Row> inputData = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("subscribe", inputTopicName)
                .load()
                .select(from_json(col("value").cast(DataTypes.StringType), messageSchema).alias("parsed_value"))
                .select("parsed_value.*")
                .withColumn("timestamp", from_unixtime(col("timestamp")).cast(DataTypes.TimestampType));

        inputData.printSchema();
        /*inputData
                .writeStream()
                .format("console")
                .outputMode(OutputMode.Append())
                .start();*/


        // aggregations

        /*
        // alternative way in mostly-SQL
        inputData
                .withWatermark("timestamp", "30 minutes")
                .createTempView("inputTable");
        Dataset<Row> hourlySumByConsumer = inputData
                .sqlContext()
                .sql("SELECT meterID, " +
                        " named_struct('start', to_unix_timestamp(window.start), 'end', to_unix_timestamp(window.end)) as unix_window," +
                        " SUM(activeDelta) as aggregatedActiveDelta," +
                        " FROM inputTable" +
                        " GROUP BY meterID, window(timestamp, '1 hour')");
        */

        // consumer consumptions will be aggregated hourly to minimize disk usage on kafka cluster (with lower retention policy)
        Dataset<Row> hourlySumByConsumer = inputData
                .withWatermark("timestamp", "30 minutes")
                .groupBy(
                        col("meterID"),
                        col("cityID"),
                        window(col("timestamp"), "1 hour")
                )
                .sum("activeDelta")
                .withColumnRenamed("sum(activeDelta)", "aggregatedActiveDelta")
                .withColumn("unix_window", struct(
                        unix_timestamp(col("window.start")).as("start"),
                        unix_timestamp(col("window.end")).as("end")
                ))
                .select("meterID", "cityID", "unix_window", "aggregatedActiveDelta");
        hourlySumByConsumer.printSchema();


        final String totalConsumptionByCityWindowDuration = totalByCityWindowDurationSeconds + " seconds";
        Dataset<Row> totalByCity = inputData
                .withWatermark("timestamp", "5 seconds")
                .groupBy(
                        col("cityID"),
                        window(col("timestamp"), totalConsumptionByCityWindowDuration)
                )
                .sum("activeDelta")
                .withColumnRenamed("sum(activeDelta)", "aggregatedActiveDelta")
                .withColumn("unix_window", struct(
                        unix_timestamp(col("window.start")).as("start"),
                        unix_timestamp(col("window.end")).as("end")
                ))
                .join(cityCoordinates, "cityID")
                .select("cityID", "cityName", "latitude", "longitude", "unix_window", "aggregatedActiveDelta");
        totalByCity.printSchema();

        final String totalConsumptionWindowDuration = countryWindowDurationSeconds + " seconds";
        Dataset<Row> totalConsumption = inputData
                .withWatermark("timestamp", "5 seconds")
                .groupBy(
                        window(col("timestamp"), totalConsumptionWindowDuration)
                )
                .sum("activeDelta")
                .withColumnRenamed("sum(activeDelta)", "aggregatedActiveDelta")
                .withColumn("unix_window", struct(
                        unix_timestamp(col("window.start")).as("start"),
                        unix_timestamp(col("window.end")).as("end")
                ))
                .select("unix_window", "aggregatedActiveDelta");
        totalConsumption.printSchema();

        /*hourlySumByConsumer
                .writeStream()
                .format("console")
                .option("truncate", false)
                //.option("spark.sql.streaming.checkpointLocation", hourlyConsumerAggregationsCheckpointLocation)
                .outputMode(OutputMode.Update())
                .start();

        totalByCity
                .writeStream()
                .format("console")
                .option("truncate", false)
                //.option("spark.sql.streaming.checkpointLocation", cityAggregationsCheckpointLocation)
                .outputMode(OutputMode.Update())
                .start();

        totalConsumption
                .writeStream()
                .format("console")
                .option("truncate", false)
                .option("spark.sql.streaming.checkpointLocation",  countryAggregationsCheckpointLocation)
                //.outputMode(OutputMode.Update())
                .start();*/

        // TODO detect anomalies in user reporting (readers shouldn't report a lower total value than in the last report)

        StreamingQuery hourlyByConsumerQuery = hourlySumByConsumer
                .select(to_json(struct("*")).alias("value"), col("meterID").cast(DataTypes.StringType).alias("key"))
                .writeStream()
                .outputMode(OutputMode.Update())
                .format("kafka")
                //.option("truncate", false)
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("topic", hourlyConsumerTopic)
                .option("checkpointLocation", hourlyConsumerAggregationsCheckpointLocation)
                .start();
        StreamingQuery totalByCityQuery = totalByCity
                .select(to_json(struct("*")).alias("value"), col("cityID").cast(DataTypes.StringType).alias("key"))
                .writeStream()
                .outputMode(OutputMode.Update())
                .format("kafka")
                //.option("truncate", false)
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("topic", totalByCityTopic)
                .option("checkpointLocation", cityAggregationsCheckpointLocation)
                .start();
        StreamingQuery totalConsumptionQuery = totalConsumption
                .select(to_json(struct("*")).alias("value"))
                .writeStream()
                .outputMode(OutputMode.Update())
                .format("kafka")
                //.option("truncate", false)
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("topic", totalConsumptionTopic)
                .option("checkpointLocation", countryAggregationsCheckpointLocation)
                .start();

        spark.streams().awaitAnyTermination();
    }
}
