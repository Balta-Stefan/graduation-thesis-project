package baltastefan.simulator.services;

import baltastefan.simulator.models.CounterMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import javax.annotation.PostConstruct;
import java.time.ZonedDateTime;
import java.util.*;


public abstract class Simulator
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
    protected int numberOfUniqueMeters;


    @Value("${maximum-id}")
    private int maximumID;

    @Value("${midnight-to-seven-minimum-consumption}")
    private double midnightToSevenMinimumConsumption;

    @Value("${midnight-to-seven-maximum-consumption}")
    private double midnightToSevenMaximumConsumption;

    @Value("${seven-to-nine-minimum-consumption}")
    private double sevenToNineMinimumConsumption;

    @Value("${seven-to-nine-maximum-consumption}")
    private double sevenToNineMaximumConsumption;

    @Value("${nine-to-five-minimum-consumption}")
    private double nineToFiveMinimumConsumption;

    @Value("${nine-to-five-maximum-consumption}")
    private double nineToFiveMaximumConsumption;

    @Value("${five-to-midnight-minimum-consumption}")
    private double fiveToMidnightMinimumConsumption;

    @Value("${five-to-midnight-maximum-consumption}")
    private double fiveToMidnightMaximumConsumption;

    private double midnightToSevenRandomGeneratorCorrectiveFactor;
    private double sevenToNineRandomGeneratorCorrectiveFactor;
    private double nineToFiveRandomGeneratorCorrectiveFactor;
    private double fiveToMidnightRandomGeneratorCorrectiveFactor;

    @Value("${spring-seasonal-factor}")
    private double springSeasonalFactor;

    @Value("${summer-seasonal-factor}")
    private double summerSeasonalFactor;

    @Value("${autumn-seasonal-factor}")
    private double autumnSeasonalFactor;

    @Value("${winter-seasonal-factor}")
    private double winterSeasonalFactor;

    private int currentConsumerIndex = 0;

    private final List<ConsumerData> consumerData = new ArrayList<>();


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
        midnightToSevenRandomGeneratorCorrectiveFactor = midnightToSevenMaximumConsumption - midnightToSevenMinimumConsumption;
        sevenToNineRandomGeneratorCorrectiveFactor = sevenToNineMaximumConsumption - sevenToNineMinimumConsumption;
        nineToFiveRandomGeneratorCorrectiveFactor = nineToFiveMaximumConsumption - nineToFiveMinimumConsumption;
        fiveToMidnightRandomGeneratorCorrectiveFactor = fiveToMidnightMaximumConsumption - fiveToMidnightMinimumConsumption;

        List<Long> meterIds = new ArrayList<>();

        generatorUtil(numberOfUniqueMeters, meterIds);

        // map meters to cities
        long cityID = 1;
        for(int i = 0; i < numberOfUniqueMeters; i++)
        {
            Long meterId = meterIds.get(i);
            ConsumerData tempConsumerData = new ConsumerData(meterId, cityID, true);
            consumerData.add(tempConsumerData);

            cityID = (cityID + 1) % (maximumID + 1);
        }
    }

    public CounterMessage generateMessage(ZonedDateTime currentTime)
    {
        ConsumerData consumerInfo = consumerData.get(currentConsumerIndex);

        int currentHour = currentTime.getHour();

        double activeDelta = 0;

        if(currentHour < 7)
            activeDelta = midnightToSevenMinimumConsumption + Math.random()*midnightToSevenRandomGeneratorCorrectiveFactor;
        else if(currentHour < 9)
            activeDelta = sevenToNineMinimumConsumption + Math.random()*sevenToNineRandomGeneratorCorrectiveFactor;
        else if(currentHour < 17)
            activeDelta = nineToFiveMinimumConsumption + Math.random()*nineToFiveRandomGeneratorCorrectiveFactor;
        else if(currentHour <= 23)
            activeDelta = fiveToMidnightMinimumConsumption + Math.random()*fiveToMidnightRandomGeneratorCorrectiveFactor;

        int currentMonth = currentTime.getMonth().getValue();

        if(currentMonth <= 3)
            activeDelta *= winterSeasonalFactor;
        else if(currentMonth <= 6)
            activeDelta *= springSeasonalFactor;
        else if(currentMonth <= 9)
            activeDelta *= summerSeasonalFactor;
        else
            activeDelta *= autumnSeasonalFactor;

        currentConsumerIndex = (currentConsumerIndex + 1) % consumerData.size();
        return new CounterMessage(
                consumerInfo.meterID,
                consumerInfo.cityID,
                currentTime.toEpochSecond(),
                activeDelta);
    }

    public abstract void simulate();
}
