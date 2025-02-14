package com.galerija.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
public class RestTemplateConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            private final DefaultResponseErrorHandler defaultHandler = new DefaultResponseErrorHandler();

            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return response.getStatusCode().isError();
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                try {
                    HttpStatusCode statusCode = response.getStatusCode();
                    logger.error("HTTP Error: {} - {}", statusCode.value(), statusCode);
                    logger.error("Response Headers: {}", response.getHeaders());
                } catch (Exception e) {
                    logger.error("Error handling response", e);
                }
            }
        });
        return restTemplate;
    }
}
