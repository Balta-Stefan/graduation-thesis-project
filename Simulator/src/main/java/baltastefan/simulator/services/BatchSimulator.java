package baltastefan.simulator.services;

import baltastefan.simulator.models.CounterMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class BatchSimulator extends Simulator
{
    @Value("${kafka.topic.input}")
    private String inputTopicName;
    private final KafkaTemplate<String, CounterMessage> kafkaTemplate;

    public BatchSimulator(KafkaTemplate<String, CounterMessage> kafkaTemplate)
    {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct
    private void callSimulateMethod()
    {
        simulate();
    }

    @Override
    public void simulate()
    {
        ZonedDateTime time = ZonedDateTime.of(
                ZonedDateTime.now().getYear(),
                1,
                1,
                0,
                0,
                0,
                0,
                ZoneId.systemDefault());
        ZonedDateTime nextYear = time.plusYears(1);

        while(time.isBefore(nextYear))
        {
            for(int i = 0; i < numberOfUniqueMeters; i++)
            {
                CounterMessage msg = generateMessage(time);
                kafkaTemplate.send(inputTopicName, msg);
            }

            time = time.plusHours(1);
        }
    }
}
