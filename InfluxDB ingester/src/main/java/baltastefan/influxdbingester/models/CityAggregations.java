package baltastefan.influxdbingester.models;

public class CityAggregations extends Aggregation
{
    public long cityID;
    public String cityName;
    public double latitude, longitude;

    public CityAggregations(){}

    public CityAggregations(long cityID, String cityName, double latitude, double longitude, double activeDelta, double reactiveDelta, Window window)
    {
        super(activeDelta, reactiveDelta, window);
        this.cityID = cityID;
        this.cityName = cityName;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}