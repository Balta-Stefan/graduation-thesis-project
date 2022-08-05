package baltastefan.influxdbingester.models;

public class CountryAggregations extends Aggregation
{
    public CountryAggregations(){}

    public CountryAggregations(double activeDelta, double reactiveDelta, Window window)
    {
        super(activeDelta, reactiveDelta, window);
    }
}
