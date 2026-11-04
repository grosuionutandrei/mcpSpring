package dk.timer.mcp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(UrlProperties.class)
public class GantConfig {
    @Bean
    public RestClient projectsRestClient(UrlProperties urls) {
        return RestClient.builder()
                .baseUrl(urls.base())
                .build();
    }
}
