package baltastefan.influxdbingester.models;

public class HourlyConsumerAggregation extends Aggregation
{
    public long meterID;

    public HourlyConsumerAggregation(double activeDelta, double reactiveDelta, long meterID, Window window)
    {
        super(activeDelta, reactiveDelta, window);
        this.meterID = meterID;
    }
}