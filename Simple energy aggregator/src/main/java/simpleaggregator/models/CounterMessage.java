package simpleaggregator.models;

import java.io.Serializable;
import java.time.Instant;

public class CounterMessage implements Serializable
{
    public final long meterID;
    public final long cityID;
    public final Instant timestamp;
    public final double activeDelta;
    public final double reactiveDelta;

    public CounterMessage(long meterID, long cityID, Instant timestamp, double activeDelta, double reactiveDelta)
    {
        this.meterID = meterID;
        this.cityID = cityID;
        this.timestamp = timestamp;
        this.activeDelta = activeDelta;
        this.reactiveDelta = reactiveDelta;
    }
}
