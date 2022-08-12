package baltastefan.simulator;

import baltastefan.simulator.models.*;
import baltastefan.simulator.services.Simulator;
import baltastefan.simulator.testutils.TestUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.PostConstruct;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:test.properties")
/*
* These tests aren't correct for certain window lengths.For example, when using a 7-second window, Spark will introduce a 2-second difference.
* */
public class SparkAggregationsTest
{
    private final long pollTimeoutSeconds = 10;

    private KafkaConsumer<String, CityAggregations> cityAggregationsKafkaConsumer;
    private KafkaConsumer<String, CountryAggregations> countryAggregationsKafkaConsumer;
    private KafkaConsumer<String, HourlyConsumerAggregation> hourlyConsumerAggregationKafkaConsumer;

    private Set<AggregationTestingWrapper> expectedCityAggregations;
    private Set<AggregationTestingWrapper> expectedCountryAggregations;
    private Set<AggregationTestingWrapper> expectedHourlyConsumerAggregations;


    @Autowired
    private KafkaTemplate<String, CounterMessage> kafkaTemplate;
    @Autowired
    private Simulator simulator;

    @Autowired
    private TestUtils testUtils;


    @Value("${number-of-test-messages}")
    private int numberOfTestMessages;
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.key-deserializer}")
    private String keyDeserializer;

    @Value("${spring.kafka.consumer.value-deserializer}")
    private String valueDeserializer;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
    private String trustedPackages;

    @Value("${kafka.topic.input}")
    private String inputTopic;

    @Value("${kafka.topic.hourly-by-consumer}")
    private String hourlyConsumerAggregationsTopic;

    @Value("${kafka.topic.total-by-city}")
    private String aggregationsByCityTopic;

    @Value("${kafka.topic.total-consumption}")
    private String totalConsumptionTopic;

    @PostConstruct
    public void init()
    {
        Properties props = kafkaProperties();

        props.setProperty(JsonDeserializer.VALUE_DEFAULT_TYPE, CityAggregations.class.getName());
        cityAggregationsKafkaConsumer = new KafkaConsumer<>(props);
        cityAggregationsKafkaConsumer.subscribe(Collections.singletonList(aggregationsByCityTopic));

        props.setProperty(JsonDeserializer.VALUE_DEFAULT_TYPE, CountryAggregations.class.getName());
        countryAggregationsKafkaConsumer = new KafkaConsumer<>(props);
        countryAggregationsKafkaConsumer.subscribe(Collections.singletonList(totalConsumptionTopic));

        props.setProperty(JsonDeserializer.VALUE_DEFAULT_TYPE, HourlyConsumerAggregation.class.getName());
        hourlyConsumerAggregationKafkaConsumer = new KafkaConsumer<>(props);
        hourlyConsumerAggregationKafkaConsumer.subscribe(Collections.singletonList(hourlyConsumerAggregationsTopic));
    }

    private Properties kafkaProperties()
    {
        Properties props =  new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        props.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "50");

        return props;
    }

    private void sleepUtil(long milliseconds)
    {
        try
        {
            Thread.sleep(milliseconds);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Works only when window duration is less than 1 minute
     */

    @BeforeAll
    public void createMockData()
    {
        final int numberOfIterations = 3;

        Map<CityAggregations, CityAggregations> cityAggregationsTestData = new HashMap<>();
        Map<CountryAggregations,CountryAggregations> countryAggregationsTestData = new HashMap<>();
        Map<HourlyConsumerAggregation,HourlyConsumerAggregation> hourlyConsumerAggregationsTestData = new HashMap<>();


        ZonedDateTime time = ZonedDateTime.now();
        for(int j = 0; j < numberOfIterations; j++)
        {
            for (int i = 0; i < numberOfTestMessages; i++)
            {
                CounterMessage msg = simulator.generateMessage(time);
                //LocalDateTime timestamp = LocalDateTime.parse(msg.timestamp);
                //OffsetDateTime zonedTimestamp = OffsetDateTime.ofInstant(Instant.ofEpochSecond(msg.timestamp), ZoneId.systemDefault());
                msg.timestamp = time.toEpochSecond();

                kafkaTemplate.send(inputTopic, msg);

                // city aggregations
                testUtils.prepareExpectedCityAggregations(msg, cityAggregationsTestData);

                // country aggregations
                testUtils.prepareExpectedCountryAggregations(msg, countryAggregationsTestData);

                // hourly aggregations by consumer
                testUtils.prepareExpectedHourlyConsumerAggregations(msg, hourlyConsumerAggregationsTestData);

            }
            //sleepUtil(3000);
            //timestamp = timestamp.plusSeconds(3).truncatedTo(ChronoUnit.SECONDS);
            time = time.plusSeconds(3);
        }

        expectedCityAggregations = cityAggregationsTestData
                .keySet()
                .stream()
                .map(AggregationTestingWrapper::new)
                .collect(Collectors.toSet());

        expectedCountryAggregations = countryAggregationsTestData
                .keySet()
                .stream()
                .map(AggregationTestingWrapper::new)
                .collect(Collectors.toSet());

        expectedHourlyConsumerAggregations = hourlyConsumerAggregationsTestData
                .keySet()
                .stream()
                .map(AggregationTestingWrapper::new)
                .collect(Collectors.toSet());


        // introduce a pause to let Spark perform all the aggregations.This is done because Spark sends aggregated data as soon as the aggregated data changes.
        sleepUtil(20000);
    }


    private <K, V extends Aggregation> void testUtility(KafkaConsumer<K, V> consumer, Set<AggregationTestingWrapper> testData)
    {
        Map<V, V> aggregations = new HashMap<>(); // key is the record value, value is the record's timestamp (obtained from Kafka record)

        ConsumerRecords<K, V> messages;

        while((messages = consumer.poll(Duration.ofSeconds(pollTimeoutSeconds))).isEmpty() == false)
        {
            for (ConsumerRecord<K, V> record : messages)
            {
                V value = record.value();

                V existingAggregation = aggregations.get(value);
                if(existingAggregation == null)
                    aggregations.put(value, value);
                else if(value.aggregatedActiveDelta > existingAggregation.aggregatedActiveDelta)
                    existingAggregation.aggregatedActiveDelta = value.aggregatedActiveDelta;
            }
        }

        Set<AggregationTestingWrapper> aggregatedData = aggregations
                .keySet()
                .stream()
                .map(AggregationTestingWrapper::new)
                .collect(Collectors.toSet());

        assertThat(aggregatedData, containsInAnyOrder(testData.toArray()));
    }

    @Test
    public void testCityAggregations()
    {
        testUtility(cityAggregationsKafkaConsumer, expectedCityAggregations);
    }

    @Test
    public void testCountryAggregations()
    {
        testUtility(countryAggregationsKafkaConsumer, expectedCountryAggregations);
    }

    @Test
    public void testHourlyConsumerAggregations()
    {
        testUtility(hourlyConsumerAggregationKafkaConsumer, expectedHourlyConsumerAggregations);
    }
}
