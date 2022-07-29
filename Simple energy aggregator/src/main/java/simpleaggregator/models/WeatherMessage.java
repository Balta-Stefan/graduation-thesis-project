package simpleaggregator.models;

public class WeatherMessage
{
    public final long cityID;
    public final long timestamp;
    public final double temperature;
    public final double windSpeed;
    public final double humidity;

    public WeatherMessage(long cityID, long timestamp, double temperature, double windSpeed, double humidity)
    {
        this.cityID = cityID;
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.humidity = humidity;
    }
}
