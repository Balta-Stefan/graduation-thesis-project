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
    private static final String hourlyConsumerTopic = "hourlyConsumer";
    private static final String totalByCityTopic = "totalByCity";
    private static final String totalConsumptionTopic = "totalConsumption";

    private static final int countryWindowDurationSeconds = 3;
    private static final int totalByCityWindowDurationSeconds = 3;

    private static final String checkpointLocation = "C:\\Users\\Korisnik\\Desktop\\Spark checkpoint\\";
    private static final String kafkaBootstrapServers = "127.0.0.1:9092";
    private static final String inputTopicName = "input";

    public static void main(String[] args) throws TimeoutException, StreamingQueryException
    {
        // https://spark.apache.org/docs/latest/submitting-applications.html#master-urls
        // https://databricks.com/blog/2017/04/26/processing-data-in-apache-kafka-with-structured-streaming-in-apache-spark-2-2.html

        // C:/Users/Korisnik/Desktop/spark-3.3.0-bin-hadoop2/bin/spark-submit.cmd --master local --deploy-mode client --class simpleaggregator.App --name Testiranje --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.3.0 "D:\Diplomski rad\Prakticni dio\Simple energy aggregator\target\simple-energy-aggregator-1.0-SNAPSHOT.jar"

        // setup

        SparkSession spark = SparkSession
                .builder()
                .appName("Simple energy aggregator Spark application")
                .config("spark.scheduler.mode", "FAIR")
                .config("spark.sql.shuffle.partitions", 5)
                .config("spark.sql.streaming.checkpointLocation", checkpointLocation)
                .config("spark.sql.session.timeZone", "UTC")
                //.master("local")
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        StructType messageSchema =  new StructType()
                .add("meterID", DataTypes.LongType, false)
                .add("cityID", DataTypes.LongType, false)
                .add("cityName", DataTypes.StringType)
                .add("timestamp", DataTypes.LongType, false)
                .add("activeDelta", DataTypes.DoubleType, false)
                .add("reactiveDelta", DataTypes.DoubleType, false);

        //messageSchema.printTreeString();

        Dataset<Row> rowData = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("subscribe", inputTopicName)
                //.option("startingOffsets", "earliest")
                .load()
                .select(from_json(col("value").cast(DataTypes.StringType), messageSchema).alias("parsed_value"))
                .select("parsed_value.*")
                .withColumn("parsed_timestamp", from_unixtime(col("timestamp")).cast(DataTypes.TimestampType));
                /*.format("json")
                .option("header", false)
                .option("multiline", true)
                .schema(messageSchema)
                .json("file:///C:\\Users\\Korisnik\\Desktop\\spark input files\\")
                .withColumn("parsed_timestamp", expr("timestamp").cast(DataTypes.TimestampType));*/
        //rowData.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)");

        rowData.printSchema();
        rowData
                .writeStream()
                .format("console")
                .outputMode(OutputMode.Append())
                .start();


        // aggregations

        // consumer consumptions will be aggregated hourly to minimize disk usage on kafka cluster (with lower retention policy)
        Dataset<Row> hourlySumByConsumer = rowData
                .withWatermark("parsed_timestamp", "30 minutes")
                .groupBy(
                        col("meterID"),
                        window(col("parsed_timestamp"), "1 hour")
                )
                .sum("activeDelta", "reactiveDelta")
                .withColumnRenamed("sum(activeDelta)", "aggregatedActiveDelta")
                .withColumnRenamed("sum(reactiveDelta)", "aggregatedReactiveDelta")
                .withColumn("unix_window", struct(
                        unix_timestamp(col("window.start")).as("start"),
                        unix_timestamp(col("window.end")).as("end")
                ))
                .select("meterID", "unix_window", "aggregatedActiveDelta", "aggregatedReactiveDelta");
        hourlySumByConsumer.printSchema();


        final String totalConsumptionByCityWindowDuration = totalByCityWindowDurationSeconds + " seconds";
        Dataset<Row> totalByCity = rowData
                .withWatermark("parsed_timestamp", "5 seconds")
                .groupBy(
                        col("cityID"),
                        window(col("parsed_timestamp"), totalConsumptionByCityWindowDuration)
                )
                .sum("activeDelta", "reactiveDelta")
                .withColumnRenamed("sum(activeDelta)", "aggregatedActiveDelta")
                .withColumnRenamed("sum(reactiveDelta)", "aggregatedReactiveDelta")
                .withColumn("unix_window", struct(
                        unix_timestamp(col("window.start")).as("start"),
                        unix_timestamp(col("window.end")).as("end")
                ))
                .select("cityID", "unix_window", "aggregatedActiveDelta", "aggregatedReactiveDelta");
        totalByCity.printSchema();

        final String totalConsumptionWindowDuration = countryWindowDurationSeconds + " seconds";
        Dataset<Row> totalConsumption = rowData
                .withWatermark("parsed_timestamp", "5 seconds")
                .groupBy(
                        window(col("parsed_timestamp"), totalConsumptionWindowDuration)
                )
                .sum("activeDelta", "reactiveDelta")
                .withColumnRenamed("sum(activeDelta)", "aggregatedActiveDelta")
                .withColumnRenamed("sum(reactiveDelta)", "aggregatedReactiveDelta")
                .withColumn("unix_window", struct(
                        unix_timestamp(col("window.start")).as("start"),
                        unix_timestamp(col("window.end")).as("end")
                ))
                .select("unix_window", "aggregatedActiveDelta", "aggregatedReactiveDelta");
        totalConsumption.printSchema();

        hourlySumByConsumer
                .writeStream()
                .format("console")
                .option("truncate", false)
                .outputMode(OutputMode.Update())
                .start();

        totalByCity
                .writeStream()
                .format("console")
                .option("truncate", false)
                .outputMode(OutputMode.Update())
                .start();

        totalConsumption
                .writeStream()
                .format("console")
                .option("truncate", false)
                .outputMode(OutputMode.Update())
                .start();

        // TODO detect anomalies in user reporting (readers shouldn't report a lower total value than in the last report)

        //totalByCity.printSchema();

        StreamingQuery hourlyByConsumerQuery = hourlySumByConsumer
                .select(to_json(struct("*")).alias("value"))
                .writeStream()
                .outputMode(OutputMode.Update())
                .format("kafka")
                //.option("truncate", false)
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("topic", hourlyConsumerTopic)
                .start();
        StreamingQuery totalByCityQuery = totalByCity
                .select(to_json(struct("*")).alias("value"))
                .writeStream()
                .outputMode(OutputMode.Update())
                .format("kafka")
                //.option("truncate", false)
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("topic", totalByCityTopic)
                .start();
        StreamingQuery totalConsumptionQuery = totalConsumption
                .select(to_json(struct("*")).alias("value"))
                .writeStream()
                .outputMode(OutputMode.Update())
                .format("kafka")
                //.option("truncate", false)
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("topic", totalConsumptionTopic)
                .start();

        spark.streams().awaitAnyTermination();
        /*query1.awaitTermination();
        System.out.println("======================= behind query1.awaitTermination");
        query2.awaitTermination();
        System.out.println("======================= behind query2.awaitTermination");
        query3.awaitTermination();*/
    }
}
