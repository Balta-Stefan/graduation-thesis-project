package baltastefan.influxdbingester.models;

public class HourlyConsumerAggregation extends Aggregation
{
    public long meterID;

    public HourlyConsumerAggregation(double activeDelta, long meterID, Window window)
    {
        super(activeDelta, window);
        this.meterID = meterID;
    }
}