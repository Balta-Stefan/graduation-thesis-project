package baltastefan.influxdbingester.models;

public class Window
{
    public long start, end;

    public Window(){}

    public Window(long start, long end)
    {
        this.start = start;
        this.end = end;
    }
}