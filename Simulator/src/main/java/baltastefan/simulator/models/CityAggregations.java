package baltastefan.simulator.models;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, exclude = {"latitude", "longitude"})
public class CityAggregations extends Aggregation
{
    public long cityID;
    public String cityName;
    public double latitude, longitude;

    public CityAggregations(long cityID, String cityName, double latitude, double longitude, double activeDelta, Window window)
    {
        super(activeDelta, window);
        this.cityID = cityID;
        this.cityName = cityName;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
