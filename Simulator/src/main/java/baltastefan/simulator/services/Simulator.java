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
    private static class ConsumerData
    {
        public final long meterID;
        public final long cityID;
        public final boolean isHousehold; // false means that the consumer is industrial

        public ConsumerData(long meterID, long cityID, boolean isHousehold)
        {
            this.meterID = meterID;
            this.cityID = cityID;
            this.isHousehold = isHousehold;
        }
    }
    @Value("${number-of-unique-meters}")
    private int numberOfUniqueMeters;


    @Value("${maximum-id}")
    private int maximumID;

    @Value("${kafka.topic.input}")
    private String inputTopicName;

    @Value("${number-of-messages-per-interval}")
    private int numberOfMessagesPerInterval;

    @Value("${minimum-household-power-consumption-per-measuring-period}")
    private double minimumHouseholdConsumptionPerPeriod;

    @Value("${maximum-household-power-consumption-per-measuring-period}")
    private double maximumHouseholdConsumptionPerPeriod;

    @Value("${percentage-of-industry-consumers}")
    private double percentageOfIndustryConsumers;

    private int currentConsumerIndex = 0;

    private final List<ConsumerData> consumerData = new ArrayList<>();
    private final KafkaTemplate<String, CounterMessage> kafkaTemplate;

    private double householdRandomGeneratorFactor;

    public Simulator(KafkaTemplate<String, CounterMessage> kafkaTemplate)
    {
        this.kafkaTemplate = kafkaTemplate;
    }

    private void generatorUtil(int numberOfUniqueIds, List<Long> list)
    {
        Random rnd = new Random();
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
        List<Long> meterIds = new ArrayList<>();

        householdRandomGeneratorFactor = 1/(maximumHouseholdConsumptionPerPeriod - minimumHouseholdConsumptionPerPeriod);
        generatorUtil(numberOfUniqueMeters, meterIds);

        // map meters to cities
        long cityID = 1;
        for(int i = 0; i < numberOfUniqueMeters; i++)
        {
            Long meterId = meterIds.get(i);
            boolean isConsumer = (Math.random() <= percentageOfIndustryConsumers/100) ? false : true;
            ConsumerData tempConsumerData = new ConsumerData(meterId, cityID, isConsumer);
            consumerData.add(tempConsumerData);

            cityID = (cityID + 1) % (maximumID + 1);
        }
    }

    /**
     * It is assumed that the average daily household power consumption is 12.5 kWh.This means that between 0.2W and 0.7W will be consumed every 3 seconds.
     * */
    public synchronized CounterMessage generateMessage()
    {
        long currentTimestamp = Instant.now().getEpochSecond();

        double activeDelta = minimumHouseholdConsumptionPerPeriod + Math.random() / householdRandomGeneratorFactor; // minimum and maximum values are given in the properties file
        ConsumerData consumerInfo = consumerData.get(currentConsumerIndex);

        if(consumerInfo.isHousehold)
        {

        }
        else
        {

        }

        currentConsumerIndex = (currentConsumerIndex + 1) % consumerData.size();
        return new CounterMessage(
                consumerInfo.meterID,
                consumerInfo.cityID,
                currentTimestamp,
                activeDelta);
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
