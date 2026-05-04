package com.cliniradar.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({AppProperties.class, PubMedProperties.class})
public class HttpClientConfig {

    @Bean
    @Primary
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .defaultHeader(HttpHeaders.USER_AGENT, "cliniradar/0.1")
                .build();
    }

    @Bean("ollamaRestClient")
    public RestClient ollamaRestClient(RestClient.Builder builder, AppProperties appProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(appProperties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(appProperties.getReadTimeoutMs()));

        return builder
                .requestFactory(requestFactory)
                .build();
    }
}
