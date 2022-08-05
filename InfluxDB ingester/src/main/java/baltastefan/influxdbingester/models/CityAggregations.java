package baltastefan.influxdbingester.models;

public class CityAggregations extends Aggregation
{
    public long cityID;
    public double latitude, longitude;

    public CityAggregations(){}

    public CityAggregations(double activeDelta, double reactiveDelta, long cityID, double latitude, double longitude, Window window)
    {
        super(activeDelta, reactiveDelta, window);
        this.cityID = cityID;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}