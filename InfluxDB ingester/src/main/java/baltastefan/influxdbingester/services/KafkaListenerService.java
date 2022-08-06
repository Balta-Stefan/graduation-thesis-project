package baltastefan.influxdbingester.services;

import baltastefan.influxdbingester.models.CityAggregations;
import baltastefan.influxdbingester.models.CountryAggregations;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KafkaListenerService
{
    private final WriteApi influxDbWriter;

    public KafkaListenerService(WriteApi influxDbWriter)
    {
        this.influxDbWriter = influxDbWriter;
    }


    @KafkaListener(topics = "${kafka.topic.total-by-city}", groupId = "${influxdb-ingester-group-id}", properties = "spring.json.value.default.type=baltastefan.influxdbingester.models.CityAggregations")
    public void handleCityAggregations(List<CityAggregations> cityAggregations)
    {
        List<Point> points = new ArrayList<>();
        for(CityAggregations agg : cityAggregations)
        {
            // add coordinates later
            Point point = Point.measurement("city-aggregation")
                    .time(agg.unix_window.end, WritePrecision.S)
                    .addField("aggregatedActiveDelta", agg.aggregatedActiveDelta)
                    .addField("aggregatedReactiveDelta", agg.aggregatedReactiveDelta)
                    .addField("latitude", agg.latitude)
                    .addField("longitude", agg.longitude)
                    .addTag("cityID", Long.toString(agg.cityID))
                    .addTag("cityName", agg.cityName);
            points.add(point);
        }

        System.out.println("sending city aggregations");
        influxDbWriter.writePoints(points);
    }

    @KafkaListener(topics = "${kafka.topic.total-consumption}", groupId = "${influxdb-ingester-group-id}", properties = "spring.json.value.default.type=baltastefan.influxdbingester.models.CountryAggregations")
    public void handleCountryAggregations(List<CountryAggregations> countryAggregations)
    {
        List<Point> points = new ArrayList<>();
        for(CountryAggregations agg : countryAggregations)
        {
            Point point = Point.measurement("country-aggregation")
                    .time(agg.unix_window.end, WritePrecision.S)
                    .addField("aggregatedActiveDelta", agg.aggregatedActiveDelta)
                    .addField("aggregatedReactiveDelta", agg.aggregatedReactiveDelta);
            points.add(point);
        }
        System.out.println("sending country aggregations");
        influxDbWriter.writePoints(points);
    }
}
