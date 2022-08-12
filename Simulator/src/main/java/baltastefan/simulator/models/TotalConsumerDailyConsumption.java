package baltastefan.simulator.models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;


@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TotalConsumerDailyConsumption
{
    public long meterID;
    public long cityID;
    public String date;
    public double aggregatedActiveDelta;
}
