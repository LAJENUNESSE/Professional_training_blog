package com.example.blog.config;

import com.example.blog.search.SearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SearchProperties.class)
public class SearchConfig {

    @Bean
    public RestClient meilisearchRestClient(RestClient.Builder builder, SearchProperties properties) {
        RestClient.Builder restBuilder = builder.baseUrl(properties.getMeilisearch().getHost());
        String apiKey = properties.getMeilisearch().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            restBuilder.defaultHeader("X-Meili-API-Key", apiKey);
        }
        return restBuilder.build();
    }
}
