package baltastefan.simulator;

import baltastefan.simulator.models.Aggregation;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.Objects;

@AllArgsConstructor
@ToString
public class AggregationTestingWrapper
{
    public Aggregation aggregation;

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregationTestingWrapper that = (AggregationTestingWrapper) o;
        if(this.aggregation.aggregatedActiveDelta != that.aggregation.aggregatedActiveDelta || this.aggregation.aggregatedReactiveDelta != that.aggregation.aggregatedReactiveDelta)
            return false;

        return aggregation.equals(that.aggregation);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(aggregation, aggregation.aggregatedActiveDelta, aggregation.aggregatedReactiveDelta);
    }
}
