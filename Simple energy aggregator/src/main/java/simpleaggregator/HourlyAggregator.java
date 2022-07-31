package simpleaggregator;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.OutputMode;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;
import static org.apache.spark.sql.functions.col;

public class HourlyAggregator
{
    private static final String topicName = "hourlyConsumptionByConsumer";
    private static final String checkpointLocation = "s3a://testing/hourly-aggregator-checkpoints";
    private static final String kafkaBootstrapServers = "127.0.0.1:9092";

    public static void main(String[] args) throws TimeoutException
    {
        SparkSession spark = SparkSession
                .builder()
                .appName("Simple energy aggregator Spark application")
                .config("spark.scheduler.mode", "FAIR")
                .config("spark.sql.shuffle.partitions", 5)
                .config("spark.sql.streaming.checkpointLocation", checkpointLocation)
                .config("spark.sql.session.timeZone", "UTC")
                .getOrCreate();


        StructField[] unixWindowFields = new StructField[2];
        unixWindowFields[0] = new StructField("start", DataTypes.LongType, false, Metadata.empty());
        unixWindowFields[1] = new StructField("end", DataTypes.LongType, false, Metadata.empty());


        StructType messageSchema =  new StructType()
                .add("meterID", DataTypes.LongType, false)
                .add("aggregatedActiveDelta", DataTypes.DoubleType, false)
                .add("aggregatedReactiveDelta", DataTypes.DoubleType, false)
                .add("unix_window", DataTypes.createStructType(unixWindowFields), false);
        messageSchema.printTreeString();

        Dataset<Row> rowData = spark
                .read()
                .format("kafka")
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("subscribe", topicName)
                .option("startingOffsets", "earliest")
                .load()
                .select(from_json(col("value").cast(DataTypes.StringType), messageSchema).alias("parsed_value"))
                .select("parsed_value.*");

        rowData.printSchema();
        rowData
                .write()
                .format("console")
                .mode(SaveMode.Append)
                .save();
    }
}
