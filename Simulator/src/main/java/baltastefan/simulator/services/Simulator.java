package baltastefan.simulator.services;

import baltastefan.simulator.models.CounterMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;


@Service
public class Simulator
{
    @Value("${number-of-unique-meters}")
    private int numberOfUniqueMeters;


    @Value("${maximum-id}")
    private int maximumID;

    @Value("${kafka.topic.input}")
    private String inputTopicName;

    @Value("${number-of-messages-per-interval}")
    private int numberOfMessagesPerInterval;

    private List<Long> meterIds = new ArrayList<>();
    private Map<Long, Long> meterIdToCityIdMapper = new HashMap<>(); // key is meterId, value is cityId
    private final Map<Long, Double> totalConsumed = new HashMap<>(); // key is meterId

    private final Random rnd = new Random();
    private final KafkaTemplate<String, CounterMessage> kafkaTemplate;

    public Simulator(KafkaTemplate<String, CounterMessage> kafkaTemplate)
    {
        this.kafkaTemplate = kafkaTemplate;
    }

    private void generatorUtil(int numberOfUniqueIds, List<Long> list)
    {
        rnd
                .longs()
                .map(Math::abs)
                .distinct()
                .limit(numberOfUniqueIds)
                .forEach(list::add);
    }

    @PostConstruct
    private void generateData()
    {
        generatorUtil(numberOfUniqueMeters, meterIds);

        // map meters to cities
        long cityID = 1;
        for(int i = 0; i < numberOfUniqueMeters; i++)
        {
            Long meterId = meterIds.get(i);
            meterIdToCityIdMapper.put(meterId, cityID);

            cityID = (cityID + 1) % (maximumID + 1);
        }
    }

    public synchronized CounterMessage generateMessage()
    {
        long currentTimestamp = Instant.now().getEpochSecond();

        double activeDelta = rnd.nextInt(10);
        double reactiveDelta = rnd.nextInt(10);
        Long meterId = meterIds.get(rnd.nextInt(meterIds.size()));
        Long cityId = meterIdToCityIdMapper.get(meterId);

        Double consumedSoFar = totalConsumed.get(meterId);
        if(consumedSoFar == null)
            consumedSoFar = 0.0;

        consumedSoFar += activeDelta + reactiveDelta;
        totalConsumed.put(meterId, consumedSoFar);

        return new CounterMessage(
                meterId,
                cityId,
                currentTimestamp,
                activeDelta,
                reactiveDelta,
                consumedSoFar);
    }

    @Scheduled(fixedDelayString = "${scheduling-rate-ms}")
    public void simulate()
    {
        for(int i = 0; i < numberOfMessagesPerInterval; i++)
        {
            CounterMessage msg = generateMessage();
            kafkaTemplate.send(inputTopicName, msg);
        }
    }
}
