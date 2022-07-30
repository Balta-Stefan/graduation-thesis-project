package baltastefan.simulator.testutils;

import baltastefan.simulator.models.*;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public class TestUtils
{
    @Value("${city-aggregations-window-duration-seconds}")
    private static int cityAggregationsWindowDurationSeconds;
    @Value("${country-aggregations-window-duration-seconds}")
    private static int countryAggregationsWindowDurationSeconds;
    private static OffsetDateTime calculateLowerWindowBoundForSecondWindows(OffsetDateTime eventTimestamp, int windowDurationSeconds)
    {
        // windowStart = 60 - ceil((60-eventSecond)/windowDuration)*windowDuration
        int windowStartSecond = (int)(60.0 - Math.ceil((60.0 - eventTimestamp.getSecond()) / windowDurationSeconds)*windowDurationSeconds);
        OffsetDateTime windowStartTimestamp;

        if(windowStartSecond < 0)
        {
            // window start is in the previous minute
            windowStartSecond = 60 + windowStartSecond;
            windowStartTimestamp = eventTimestamp.minusMinutes(1).withSecond(windowStartSecond);
        }
        else
        {
            windowStartTimestamp = eventTimestamp.withSecond(windowStartSecond);
        }

        return windowStartTimestamp;
    }


    public static void prepareExpectedHourlyConsumerAggregations(CounterMessage msg, Map<HourlyConsumerAggregation,HourlyConsumerAggregation> hourlyConsumerAggregationsTestData)
    {
        // window is aligned by the hour
        OffsetDateTime windowStart = OffsetDateTime
                .ofInstant(Instant.ofEpochSecond(msg.timestamp), ZoneOffset.systemDefault())
                .withMinute(0)
                .withSecond(0);
        OffsetDateTime windowEnd = windowStart.plusHours(1);

        Window window = new Window(windowStart.toEpochSecond(), windowEnd.toEpochSecond());
        HourlyConsumerAggregation hourlyAggregation = new HourlyConsumerAggregation(msg.activeDelta, msg.reactiveDelta, msg.meterID, window);
        HourlyConsumerAggregation tmpHourly = hourlyConsumerAggregationsTestData.get(hourlyAggregation);
        if(tmpHourly == null)
        {
            hourlyConsumerAggregationsTestData.put(hourlyAggregation, hourlyAggregation);
        }
        else
        {
            tmpHourly.aggregatedActiveDelta += hourlyAggregation.aggregatedActiveDelta;
            tmpHourly.aggregatedReactiveDelta += hourlyAggregation.aggregatedReactiveDelta;
        }
    }

    public static void prepareExpectedCountryAggregations(CounterMessage msg, Map<CountryAggregations,CountryAggregations> countryAggregationsTestData)
    {
        OffsetDateTime eventTimestamp = OffsetDateTime
                .ofInstant(Instant.ofEpochSecond(msg.timestamp), ZoneOffset.systemDefault());

        OffsetDateTime windowStart = calculateLowerWindowBoundForSecondWindows(eventTimestamp, countryAggregationsWindowDurationSeconds);
        OffsetDateTime windowEnd = windowStart.plusSeconds(countryAggregationsWindowDurationSeconds);

        Window window = new Window(windowStart.toEpochSecond(), windowEnd.toEpochSecond());
        CountryAggregations aggregation = new CountryAggregations(msg.activeDelta, msg.reactiveDelta, window);
        CountryAggregations tmpAggregation = countryAggregationsTestData.get(aggregation);
        if(tmpAggregation == null)
        {
            countryAggregationsTestData.put(aggregation, aggregation);
        }
        else
        {
            tmpAggregation.aggregatedActiveDelta += aggregation.aggregatedActiveDelta;
            tmpAggregation.aggregatedReactiveDelta += aggregation.aggregatedReactiveDelta;
        }
    }

    public static void prepareExpectedCityAggregations(CounterMessage msg, Map<CityAggregations, CityAggregations> cityAggregationsTestData)
    {
        OffsetDateTime eventTimestamp = OffsetDateTime
                .ofInstant(Instant.ofEpochSecond(msg.timestamp), ZoneOffset.systemDefault());

        OffsetDateTime windowStart = calculateLowerWindowBoundForSecondWindows(eventTimestamp, cityAggregationsWindowDurationSeconds);
        OffsetDateTime windowEnd = windowStart.plusSeconds(cityAggregationsWindowDurationSeconds);

        Window window = new Window(windowStart.toEpochSecond(), windowEnd.toEpochSecond());
        CityAggregations aggregation = new CityAggregations(msg.activeDelta, msg.reactiveDelta, msg.cityID, window);
        CityAggregations tmpAggregation =  cityAggregationsTestData.get(aggregation);
        if(tmpAggregation == null)
        {
            cityAggregationsTestData.put(aggregation, aggregation);
        }
        else
        {
            tmpAggregation.aggregatedActiveDelta += aggregation.aggregatedActiveDelta;
            tmpAggregation.aggregatedReactiveDelta += aggregation.aggregatedReactiveDelta;
        }
    }
}
