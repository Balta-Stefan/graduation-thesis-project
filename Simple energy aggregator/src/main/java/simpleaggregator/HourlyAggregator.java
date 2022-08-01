package simpleaggregator;

import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import static org.apache.spark.sql.functions.*;
import static org.apache.spark.sql.functions.col;

public class HourlyAggregator
{
    private static final String topicName = "hourlyConsumptionByConsumer";
    private static final String checkpointLocation = "s3a://testing/hourly-aggregator-checkpoints";
    private static final String kafkaBootstrapServers = "127.0.0.1:9092";

    private static final String s3Endpoint = "localhost:9000";

    public static void main(String[] args)
    {
        SparkSession spark = SparkSession
                .builder()
                .appName("Simple energy aggregator Spark application")
                .config("spark.scheduler.mode", "FAIR")
                .config("spark.sql.shuffle.partitions", 5)
                .config("spark.sql.streaming.checkpointLocation", checkpointLocation)
                .config("spark.sql.session.timeZone", "UTC")
                .config("spark.hadoop.fs.s3a.endpoint", s3Endpoint)
                .config("spark.hadoop.fs.s3a.access.key", "vQStvk5ileb3Mhw0")
                .config("spark.hadoop.fs.s3a.secret.key", "4rhALdLpPa8IBXxDehI69Ki3613krErB")
                .config("spark.hadoop.fs.s3a.committer.name", "directory")
                .config("spark.sql.sources.commitProtocolClass", "org.apache.spark.internal.io.cloud.PathOutputCommitProtocol")
                .config("spark.sql.parquet.output.committer.class", "org.apache.spark.internal.io.cloud.BindingParquetOutputCommitter")
                .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
                .config("spark.hadoop.fs.s3a.path.style.access", true)
                //.config("spark.hadoop.fs.s3a.connection.timeout", 10)
                .config("spark.hadoop.fs.s3a.connection.ssl.enabled", false)
                .master("local")
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        // this is required for testing
        final String s3OutputPath = args[0];
        final String outputType = args[1].toLowerCase();

        /*Dataset<Row> testJsonData = spark
                .read()
                .option("multiline", true)
                .json("s3a://simple-energy-aggregator/test_data.json");
        testJsonData.show();
        System.out.println("after reading test json file");

        testJsonData
                .write()
                .mode(SaveMode.Overwrite)
                .parquet(outputLocation);*/


        StructField[] unixWindowFields = new StructField[2];
        unixWindowFields[0] = new StructField("start", DataTypes.LongType, false, Metadata.empty());
        unixWindowFields[1] = new StructField("end", DataTypes.LongType, false, Metadata.empty());


        StructType messageSchema =  new StructType()
                .add("meterID", DataTypes.LongType, false)
                .add("aggregatedActiveDelta", DataTypes.DoubleType, false)
                .add("aggregatedReactiveDelta", DataTypes.DoubleType, false)
                .add("unix_window", DataTypes.createStructType(unixWindowFields), false);
        messageSchema.printTreeString();

        Dataset<Row> inputData = spark
                .read()
                .format("kafka")
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("subscribe", topicName)
                .option("startingOffsets", "earliest")
                .load()
                .select(from_json(col("value").cast(DataTypes.StringType), messageSchema).alias("parsed_value"))
                .select("parsed_value.*");

        inputData.printSchema();

        DataFrameWriter<Row> data = inputData
                .write()
                .mode(SaveMode.Append);


        if(outputType.equals("json"))
            data.json("s3a://" + s3OutputPath);
        else
            data.parquet("s3a://" + s3OutputPath);

        inputData
                .write()
                .format("console")
                .mode(SaveMode.Append)
                .save();
    }
}
