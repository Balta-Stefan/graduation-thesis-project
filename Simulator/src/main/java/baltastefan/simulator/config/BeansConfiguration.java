package baltastefan.simulator.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class BeansConfiguration
{
    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.port}")
    private int minioEndpointPort;

    @Value("${minio.access_key}")
    private String accessKey;

    @Value("${minio.secret_access_key}")
    private String secret;

    @Bean
    public MinioClient minioClient()
    {
        return MinioClient.builder()
                .endpoint(minioEndpoint, minioEndpointPort, false)
                .credentials(accessKey, secret)
                .build();
    }
}
