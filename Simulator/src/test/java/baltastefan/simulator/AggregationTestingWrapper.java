package baltastefan.simulator;

import baltastefan.simulator.models.Aggregation;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@ToString
public class AggregationTestingWrapper
{
    public Aggregation aggregation;

    public AggregationTestingWrapper(Aggregation aggregation)
    {
        this.aggregation = aggregation;
        this.aggregation.aggregatedActiveDelta = new BigDecimal(Double.toString(this.aggregation.aggregatedActiveDelta)).setScale(5, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregationTestingWrapper that = (AggregationTestingWrapper) o;
        if(this.aggregation.aggregatedActiveDelta != that.aggregation.aggregatedActiveDelta)
            return false;

        return aggregation.equals(that.aggregation);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(aggregation, aggregation.aggregatedActiveDelta);
    }
}
