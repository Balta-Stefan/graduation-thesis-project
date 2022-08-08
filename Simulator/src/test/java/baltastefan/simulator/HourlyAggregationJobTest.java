package baltastefan.simulator;

import baltastefan.simulator.models.CounterMessage;
import baltastefan.simulator.models.HourlyConsumerAggregation;
import baltastefan.simulator.services.Simulator;
import baltastefan.simulator.testutils.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.*;
import io.minio.messages.Item;
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
public class HourlyAggregationJobTest
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


    // send messages
    // poll MinIO for a JSON object (file)

    @Test
    public void testHourlyAggregations() throws Exception
    {
        Map<HourlyConsumerAggregation, HourlyConsumerAggregation> aggregations = new HashMap<>();

        ZonedDateTime time = ZonedDateTime.now();
        for(int i = 0; i < numberOfTestMessages; i++)
        {
            CounterMessage msg = simulator.generateMessage(time);
            TestUtils.prepareExpectedHourlyConsumerAggregations(msg, aggregations);
            kafkaTemplate.send(inputTopic, msg);
            time = time.plusSeconds(3);
        }
        AggregationTestingWrapper[] testAggregations =
                aggregations
                        .keySet()
                        .stream()
                        .map(AggregationTestingWrapper::new)
                        .toArray(AggregationTestingWrapper[]::new);


        long tryUntil = Instant.now().getEpochSecond() + minioPollingTimeoutSeconds;

        ObjectMapper objectMapper = new ObjectMapper();

        while(true)
        {
            Thread.sleep(1000);
            if(Instant.now().getEpochSecond() > tryUntil)
                throw new RuntimeException("Timeout period has run out!");

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(aggregatedResultsObjectPrefix)
                            .build()
            );

            Item jsonFile = null;
            for(Result<Item> r : results)
            {
                System.out.println(r.get().objectName());
                if(r.get().objectName().endsWith(".json"))
                {
                    jsonFile = r.get();
                    break;
                }
            }

            if(jsonFile == null)
                continue;


            Set<AggregationTestingWrapper> deserializedAggregations = new HashSet<>();

            try(InputStream is = minioClient
                    .getObject(GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(jsonFile.objectName())
                            .build());
                BufferedReader br = new BufferedReader(new InputStreamReader(is)))
            {
                String currentLine;
                while((currentLine = br.readLine()) != null)
                {
                    HourlyConsumerAggregation tempAgg = objectMapper.readValue(currentLine, HourlyConsumerAggregation.class);
                    deserializedAggregations.add(new AggregationTestingWrapper(tempAgg));
                }

                // Spark creates a directory with 2 files.It will be deleted here

                for(Result<Item> r : results)
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


            System.out.println("Displaying test data");
            for(AggregationTestingWrapper tmp : testAggregations)
                System.out.println(tmp);

            System.out.println();
            System.out.println("Displaying received data");
            for(AggregationTestingWrapper tmp : deserializedAggregations)
                System.out.println(tmp);

            assertThat(deserializedAggregations, containsInAnyOrder(testAggregations));
            return;
        }
    }
}
