package baltastefan.simulator.models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(exclude = {"aggregatedActiveDelta"})
public class Aggregation
{
    public double aggregatedActiveDelta;
    public Window unix_window;
}
