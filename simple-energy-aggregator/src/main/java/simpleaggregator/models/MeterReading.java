package simpleaggregator.models;

import java.io.Serializable;
import java.time.Instant;

public class MeterReading implements Serializable
{
    public final long meterID;
    public final long cityID;
    public final Instant timestamp;
    public final double activeDelta;

    public MeterReading(long meterID, long cityID, Instant timestamp, double activeDelta)
    {
        this.meterID = meterID;
        this.cityID = cityID;
        this.timestamp = timestamp;
        this.activeDelta = activeDelta;
    }
}
