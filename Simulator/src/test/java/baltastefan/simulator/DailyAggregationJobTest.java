package baltastefan.simulator;

import baltastefan.simulator.models.CounterMessage;
import baltastefan.simulator.models.HourlyConsumerAggregation;
import baltastefan.simulator.models.TotalConsumerDailyConsumption;
import baltastefan.simulator.services.Simulator;
import baltastefan.simulator.testutils.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.*;
import io.minio.messages.Item;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

@SpringBootTest
@TestPropertySource(locations = "classpath:test.properties")
public class DailyAggregationJobTest
{
    @Value("${number-of-test-messages}")
    private int numberOfTestMessages;

    @Value("${hourly-aggregation-job-minio-poll-timeout-seconds}")
    private int minioPollingTimeoutSeconds;

    @Value("${kafka.topic.input}")
    private String inputTopic;

    @Value("${test-bucket}")
    private String bucketName;

    @Value("${hourly-aggregation-test-prefix}")
    private String aggregatedResultsObjectPrefix;

    @Autowired
    private MinioClient minioClient;
    @Autowired
    private Simulator simulator;

    @Autowired
    private KafkaTemplate<String, CounterMessage> kafkaTemplate;



    private List<TotalConsumerDailyConsumption> generateTestData()
    {
        @EqualsAndHashCode
        @AllArgsConstructor
        class MapPair
        {
            public long meterID;
            public String date;
        }
        Map<MapPair, TotalConsumerDailyConsumption> aggregations = new HashMap<>();

        ZonedDateTime time = ZonedDateTime.now();
        ZonedDateTime nextDay = time.plusDays(1);

        while(time.isBefore(nextDay))
        {
            CounterMessage msg = simulator.generateMessage(time);

            MapPair tempPair = new MapPair(msg.meterID, time.toLocalDate().toString());
            TotalConsumerDailyConsumption existingAggregation = aggregations.get(tempPair);
            if(existingAggregation == null)
                aggregations.put(tempPair, new TotalConsumerDailyConsumption(msg.meterID, time.toLocalDate().toString(), msg.activeDelta));
            else
                existingAggregation.aggregatedActiveDelta += msg.activeDelta;

            kafkaTemplate.send(inputTopic, msg);
            time = time.plusSeconds(60);
        }

        return new ArrayList<TotalConsumerDailyConsumption>(aggregations.values());
    }

    private Iterable<Result<Item>> foundObjects;

    private List<Item> fetchAggregationsFromObjectStorage() throws Exception
    {
        foundObjects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(aggregatedResultsObjectPrefix)
                        .build()
        );

        List<Item> items = new ArrayList<>();

        for(Result<Item> r : foundObjects)
        {
            System.out.println(r.get().objectName());
            if(r.get().objectName().endsWith(".json"))
            {
                items.add(r.get());
            }
        }

        return items;
    }

    private void clearTestObjects() throws Exception
    {
        // Spark creates a directory with 2 files.It will be deleted here
        for(Result<Item> r : foundObjects)
        {
            minioClient
                    .removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(r.get().objectName())
                                    .build()
                    );
        }
    }

    private List<TotalConsumerDailyConsumption> deserializeAggregatedData(List<Item> jsonFiles) throws Exception
    {
        List<TotalConsumerDailyConsumption> deserializedAggregations = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        for(Item file : jsonFiles)
        {
            try (InputStream is = minioClient
                    .getObject(GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(file.objectName())
                            .build());
                 BufferedReader br = new BufferedReader(new InputStreamReader(is)))
            {
                String currentLine;
                while ((currentLine = br.readLine()) != null)
                {
                    TotalConsumerDailyConsumption tempAgg = objectMapper.readValue(currentLine, TotalConsumerDailyConsumption.class);
                    deserializedAggregations.add(tempAgg);
                }
            }
        }
        clearTestObjects();

        return deserializedAggregations;
    }

    @Test
    public void testDailyAggregations() throws Exception
    {
        List<TotalConsumerDailyConsumption> testAggregations = generateTestData();

        long tryUntil = Instant.now().getEpochSecond() + minioPollingTimeoutSeconds;


        List<Item> jsonFiles;
        do
        {
            Thread.sleep(1000);
            if (Instant.now().getEpochSecond() > tryUntil)
                throw new RuntimeException("Timeout period has run out!");

            jsonFiles = fetchAggregationsFromObjectStorage();

        } while (jsonFiles.size() == 0);

        List<TotalConsumerDailyConsumption> deserializedAggregations = deserializeAggregatedData(jsonFiles);


        System.out.println("Displaying test data");
        for(TotalConsumerDailyConsumption tmp : testAggregations)
            System.out.println(tmp);

        System.out.println();
        System.out.println("Displaying received data");
        for(TotalConsumerDailyConsumption tmp : deserializedAggregations)
            System.out.println(tmp);

        assertThat(deserializedAggregations, containsInAnyOrder(testAggregations));
    }
}
