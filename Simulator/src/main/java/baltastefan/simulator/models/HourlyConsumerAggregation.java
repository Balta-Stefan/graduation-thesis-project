package baltastefan.simulator.models;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class HourlyConsumerAggregation extends Aggregation
{
    public long meterID;

    public HourlyConsumerAggregation(double activeDelta, long meterID, Window window)
    {
        super(activeDelta, window);
        this.meterID = meterID;
    }
}
