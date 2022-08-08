package baltastefan.simulator.services;

import baltastefan.simulator.models.CounterMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class StreamingSimulator extends Simulator
{
    @Value("${kafka.topic.input}")
    private String inputTopicName;

    @Value("${number-of-messages-per-interval}")
    private int numberOfMessagesPerInterval;
    private final KafkaTemplate<String, CounterMessage> kafkaTemplate;

    public StreamingSimulator(KafkaTemplate<String, CounterMessage> kafkaTemplate)
    {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Scheduled(fixedDelayString = "${scheduling-rate-ms}")
    public void simulate()
    {
        ZonedDateTime time = ZonedDateTime.now();
        for(int i = 0; i < numberOfMessagesPerInterval; i++)
        {
            CounterMessage msg = generateMessage(time);
            kafkaTemplate.send(inputTopicName, msg);
        }
    }
}
