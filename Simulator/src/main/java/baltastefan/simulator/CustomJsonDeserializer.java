package baltastefan.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.support.JacksonUtils;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.text.SimpleDateFormat;

public class CustomJsonDeserializer extends JsonDeserializer<Object>
{
    public CustomJsonDeserializer()
    {
        super(customizedObjectMapper());
    }

    private static ObjectMapper customizedObjectMapper()
    {
        ObjectMapper mapper = JacksonUtils.enhancedObjectMapper();
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"));

        return mapper;
    }
}
