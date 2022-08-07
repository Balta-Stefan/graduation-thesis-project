package baltastefan.influxdbingester.models;

public class Aggregation
{
    public double aggregatedActiveDelta;
    public Window unix_window;

    public Aggregation(){}

    public Aggregation(double aggregatedActiveDelta, Window unix_window)
    {
        this.aggregatedActiveDelta = aggregatedActiveDelta;
        this.unix_window = unix_window;
    }
}
