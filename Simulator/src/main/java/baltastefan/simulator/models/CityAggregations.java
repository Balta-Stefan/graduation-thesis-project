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
    public double latitude, longitude;

    public CityAggregations(double latitude, double longitude, double activeDelta, double reactiveDelta, long cityID, Window window)
    {
        super(activeDelta, reactiveDelta, window);
        this.latitude = latitude;
        this.longitude = longitude;
        this.cityID = cityID;
    }
}
