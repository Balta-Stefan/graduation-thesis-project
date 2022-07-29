package simpleaggregator.models;

import java.io.Serializable;
import java.time.Instant;

public class CounterMessage implements Serializable
{
    public final long meterID;
    public final long cityID;
    public final String cityName;
    public final Instant timestamp;
    public final double activeDelta;
    public final double reactiveDelta;
    public final double totalConsumed;

    public CounterMessage(long meterID, long cityID, String cityName, Instant timestamp, double activeDelta, double reactiveDelta, double totalConsumed)
    {
        this.meterID = meterID;
        this.cityID = cityID;
        this.cityName = cityName;
        this.timestamp = timestamp;
        this.activeDelta = activeDelta;
        this.reactiveDelta = reactiveDelta;
        this.totalConsumed = totalConsumed;
    }
}
