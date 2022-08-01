package baltastefan.simulator.models;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CityAggregations extends Aggregation
{
    public long cityID;

    public CityAggregations(double activeDelta, double reactiveDelta, long cityID, Window window)
    {
        super(activeDelta, reactiveDelta, window);
        this.cityID = cityID;
    }
}
