package baltastefan.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.kafka.support.JacksonUtils;
import org.springframework.kafka.support.serializer.JsonSerializer;

public class CustomJsonSerializer extends JsonSerializer<Object>
{
    public CustomJsonSerializer()
    {
        super(customizedObjectMapper());
    }

    private static ObjectMapper customizedObjectMapper()
    {
        ObjectMapper mapper = JacksonUtils.enhancedObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // without this, the serializer would write DateTimes as unix timestamp

        return mapper;
    }
}
