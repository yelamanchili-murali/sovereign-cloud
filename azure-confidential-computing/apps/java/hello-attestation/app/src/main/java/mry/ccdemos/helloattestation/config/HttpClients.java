package mry.ccdemos.helloattestation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class HttpClients {

    @Bean
    public RestClient restClient() {
        var factory = new SimpleClientHttpRequestFactory();
        // Conservative timeouts for metadata/JWKS fetches
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
