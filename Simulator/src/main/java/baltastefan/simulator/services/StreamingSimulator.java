package baltastefan.simulator.services;

import baltastefan.simulator.models.MeterReading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
@Profile("streaming-simulator")
public class StreamingSimulator extends Simulator
{
    @Value("${kafka.topic.input}")
    private String inputTopicName;

    private final KafkaTemplate<String, MeterReading> kafkaTemplate;

    public StreamingSimulator(KafkaTemplate<String, MeterReading> kafkaTemplate)
    {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Scheduled(fixedDelayString = "${scheduling-rate-ms}")
    public void simulate()
    {
        ZonedDateTime time = ZonedDateTime.now();
        for(int i = 0; i < numberOfUniqueMeters; i++)
        {
            MeterReading msg = generateMessage(time);
            kafkaTemplate.send(inputTopicName, msg);
        }
    }
}
