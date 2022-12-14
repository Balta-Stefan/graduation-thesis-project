package baltastefan.simulator.models;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;


@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CountryAggregations extends Aggregation
{
    public CountryAggregations(double activeDelta, Window window)
    {
        super(activeDelta, window);
    }
}
