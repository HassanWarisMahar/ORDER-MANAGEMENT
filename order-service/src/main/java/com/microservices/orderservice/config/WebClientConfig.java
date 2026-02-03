package com.microservices.orderservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    @Value("${inventory.service.url:http://inventory-service:8082}")
    private String inventoryServiceUrl;

    @Value("${user.service.url:http://user-service:8083}")
    private String userServiceUrl;

    @Value("${gateway.internal-token:gombey-gateway-internal-token}")
    private String gatewayInternalToken;

    private volatile String cachedServiceToken;
    private volatile Instant cachedTokenExpiry;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient inventoryWebClient(WebClient.Builder webClientBuilder) {
        // Add filter to inject JWT token for each request
        ExchangeFilterFunction authFilter = ExchangeFilterFunction.ofRequestProcessor(
            clientRequest -> {
                // Fetch service token from user-service for each inter-service call
                String serviceToken = fetchServiceToken(webClientBuilder);
                ClientRequest authenticatedRequest = ClientRequest.from(clientRequest)
                        .header("Authorization", "Bearer " + serviceToken)
                        .build();
                return Mono.just(authenticatedRequest);
            }
        );
        
        return webClientBuilder
                .baseUrl(inventoryServiceUrl)
                .filter(authFilter)
                .build();
    }

    private String fetchServiceToken(WebClient.Builder webClientBuilder) {
        Instant now = Instant.now();
        String currentToken = cachedServiceToken;
        Instant currentExpiry = cachedTokenExpiry;
        if (currentToken != null && currentExpiry != null && now.isBefore(currentExpiry)) {
            return currentToken;
        }

        // Use a fresh builder to avoid reusing the inventory auth filter and causing recursion
        WebClient userServiceClient = WebClient.builder()
                .baseUrl(userServiceUrl)
                .defaultHeader("X-Gateway-Token", gatewayInternalToken)
                .build();

        TokenResponse response = userServiceClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/service-token")
                        .queryParam("serviceName", "order-service")
                        .build())
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (response == null || response.token == null || response.token.isBlank()) {
            throw new IllegalStateException("Failed to fetch service token from user-service");
        }
        cachedServiceToken = response.token;
        cachedTokenExpiry = now.plusSeconds(calculateExpirySeconds(response));
        return response.token;
    }

    private long calculateExpirySeconds(TokenResponse response) {
        if (response.expiresIn == null || response.expiresIn <= 0) {
            return 60; // safe default
        }
        // Refresh 30s early to avoid edge expiration during requests
        long earlyRefresh = Math.max(10, response.expiresIn - 30);
        return earlyRefresh;
    }

    private static class TokenResponse {
        public String token;
        public String tokenType;
        public Long expiresIn;
    }
}
