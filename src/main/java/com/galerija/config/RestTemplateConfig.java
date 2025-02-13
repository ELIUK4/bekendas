package com.galerija.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfig.class);
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .errorHandler(new ResponseErrorHandler() {
                    private final DefaultResponseErrorHandler defaultHandler = new DefaultResponseErrorHandler();
                    
                    @Override
                    public boolean hasError(ClientHttpResponse response) throws IOException {
                        return defaultHandler.hasError(response);
                    }

                    @Override
                    public void handleError(ClientHttpResponse response) throws IOException {
                        HttpStatus status = HttpStatus.valueOf(response.getRawStatusCode());
                        logger.error("HTTP Error: {} - {}", status.value(), status.getReasonPhrase());
                        logger.error("Response Headers: {}", response.getHeaders());
                        defaultHandler.handleError(response);
                    }
                })
                .build();
    }
}
