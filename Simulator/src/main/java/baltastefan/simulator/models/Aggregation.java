package baltastefan.simulator.models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(exclude = {"aggregatedActiveDelta", "aggregatedReactiveDelta"})
public class Aggregation
{
    public double aggregatedActiveDelta, aggregatedReactiveDelta;
    public Window unix_window;
}
