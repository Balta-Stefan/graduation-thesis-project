package baltastefan.influxdbingester.models;

public class CountryAggregations extends Aggregation
{
    public CountryAggregations(){}

    public CountryAggregations(double activeDelta, Window window)
    {
        super(activeDelta, window);
    }
}
