package baltastefan.simulator.services;

import baltastefan.simulator.models.CounterMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;


//@KafkaListener(topics = "output", groupId = "#{T(java.util.UUID).randomUUID().toString()}", properties = {"spring.json.use.type.headers=false"})
@Service
public class Simulator
{
    @Value("${number-of-unique-meters}")
    private int numberOfUniqueMeters;

    @Value("${number-of-unique-cities}")
    private int numberOfUniqueCities;

    private List<Long> meterIds = new ArrayList<>();
    private List<Long> cityIds = new ArrayList<>();
    private Map<Long, Long> meterIdToCityIdMapper = new HashMap<>(); // key is meterId, value is cityId
    private final Map<Long, Double> totalConsumed = new HashMap<>(); // key is meterId

    private final Random rnd = new Random();
    private final LocalDateTime currentDate = LocalDateTime.now();

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
        generatorUtil(numberOfUniqueCities, cityIds);

        // map meters to cities
        int cityIndex = 0;
        for(int i = 0; i < numberOfUniqueMeters; i++)
        {
            Long meterId = meterIds.get(i);
            meterIdToCityIdMapper.put(meterId, cityIds.get(cityIndex));

            cityIndex = (cityIndex + 1) % numberOfUniqueCities;
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
                null,
                currentTimestamp,
                activeDelta,
                reactiveDelta,
                consumedSoFar);
    }

    /*
    @PostConstruct
    public void sendMockData()
    {
        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime.now().atZone(zoneId).toEpochSecond();

        List<String> cities = Arrays.asList("Gradiska", "Banjaluka", "Sarajevo", "Zagreb", "Tuzla", "Mostar");
        List<CounterMessage> messages = new ArrayList<>();

        Random rnd = new Random();

        for(int i = 0; i < 5; i++)
        {
            CounterMessage msg = new CounterMessage(
                    Math.abs(rnd.nextLong()),
                    Math.abs(rnd.nextLong()),
                    cities.get(rnd.nextInt(cities.size())),
                    LocalDateTime.now().atZone(zoneId).toEpochSecond(),
                    rnd.nextInt(50),
                    rnd.nextInt(25),
                    rnd.nextInt(15));
            totalSimulatedActive += msg.activeDelta;
            totalSimulatedReactive += msg.reactiveDelta;
            totalSimulatedDelta += msg.totalConsumed;

            kafkaTemplate.send(inputTopic, msg);
        }
    }


    @KafkaListener(topics = "${kafka.topic.total-by-city}", groupId = "#{T(java.util.UUID).randomUUID().toString()}")
    public void readCityAggregatedData(CityAggregations cityAggregations)
    {
        System.out.println("Received CityAggregations: " + cityAggregations);
    }

    @KafkaListener(topics = "${kafka.topic.total-consumption}", groupId = "#{T(java.util.UUID).randomUUID().toString()}")
    public void readCountryAggregatedData(CountryAggregations countryAggregations)
    {
        System.out.println("Received CountryAggregations: " + countryAggregations);
    }

    @KafkaListener(topics = "${kafka.topic.hourly-by-consumer}", groupId = "#{T(java.util.UUID).randomUUID().toString()}")
    public void readHourlyConsumerAggregateData(HourlyConsumerAggregation consumerAggregation)
    {
        System.out.println("Received HourlyConsumerAggregation: " + consumerAggregation);
    }*/
}
