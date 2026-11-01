package com.kahua.assets.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class TrackerClient {

    private final RestClient restClient;

    public TrackerClient(@Value("${kahua.tracker.base-url:http://localhost:8080/api}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<Object> getTimeline(String scanCode, String token) {
        var requestSpec = restClient.get()
                .uri("/tracking/status?externalId={id}", scanCode);
        if (token != null && !token.isBlank()) {
            requestSpec = requestSpec.header("Authorization", "Bearer " + token);
        }
        return requestSpec.retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
