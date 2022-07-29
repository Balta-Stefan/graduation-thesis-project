package baltastefan.simulator.models;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;


@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class CountryAggregations extends Aggregation
{
    public CountryAggregations(double activeDelta, double reactiveDelta, Window window)
    {
        super(activeDelta, reactiveDelta, window);
    }
}
