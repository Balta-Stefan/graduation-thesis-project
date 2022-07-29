package baltastefan.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.support.converter.ByteArrayJsonMessageConverter;
import org.springframework.kafka.support.converter.JsonMessageConverter;

@SpringBootApplication
@EnableKafka
public class SimulatorApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(SimulatorApplication.class, args);
    }

    @Bean
    public JsonMessageConverter jsonMessageConverter()
    {
        return new ByteArrayJsonMessageConverter();
    }
}
