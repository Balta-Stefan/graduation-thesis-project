package baltastefan.influxdbingester.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDbConfig
{
    @Value("${influxdb.endpoint}")
    private String endpoint;

    @Value("${influxdb.token}")
    private String token;

    @Value("${influxdb.org}")
    private String org;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Bean
    public WriteApi influxDBClient()
    {
        // https://github.com/influxdata/influxdb-client-java/tree/master/client#asynchronous-non-blocking-api
        InfluxDBClient influxDBClient = InfluxDBClientFactory.create(endpoint, token.toCharArray(), org, bucket);
        return influxDBClient.makeWriteApi();
    }
}
