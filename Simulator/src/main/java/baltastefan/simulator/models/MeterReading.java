package baltastefan.simulator.models;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MeterReading
{
    public long meterID;
    public long cityID;
    public long timestamp;
    public double activeDelta;
}
