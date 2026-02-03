package com.microservices.apigateway.filter;

import com.microservices.apigateway.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Skip authentication for public endpoints
            String path = request.getURI().getPath();
            if (path.startsWith("/api/auth/signup") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/api-docs/") ||
                path.startsWith("/user-service/swagger-ui/") ||
                path.startsWith("/user-service/api-docs/") ||
                path.startsWith("/order-service/swagger-ui/") ||
                path.startsWith("/order-service/api-docs/") ||
                path.startsWith("/inventory-service/swagger-ui/") ||
                path.startsWith("/inventory-service/api-docs/") ||
                path.startsWith("/h2-console/")) {
                return chain.filter(exchange);
            }
            
            // Check for Authorization header
            String authHeader = request.getHeaders().getFirst("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {}", path);
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }
            
            try {
                String token = authHeader.substring(7);
                
                // Validate token
                if (!jwtUtil.validateToken(token)) {
                    log.warn("Invalid or expired token for path: {}", path);
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }
                
                // Extract claims for coarse-grained authorization
                String username = jwtUtil.extractUsername(token);
                String service = jwtUtil.getServiceFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                if (!isAuthorized(path, role)) {
                    log.warn("Access denied for role: {} on path: {}", role, path);
                    return onError(exchange, "Forbidden", HttpStatus.FORBIDDEN);
                }
                
                // Add user info to request headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Name", username)
                        .header("X-Service-Name", service != null ? service : "unknown")
                        .header("X-User-Role", role != null ? role : "unknown")
                        .build();
                
                log.debug("JWT token validated successfully for user: {} on path: {}", username, path);
                
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
                
            } catch (Exception e) {
                log.error("Error validating JWT token: {}", e.getMessage());
                return onError(exchange, "Error validating token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorBody = String.format("{\"status\":%d,\"message\":\"%s\",\"timestamp\":\"%s\"}", 
                status.value(), message, java.time.LocalDateTime.now());
        
        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(errorBody.getBytes()))
        );
    }

    private boolean isAuthorized(String path, String role) {
        if (role == null || role.isBlank()) {
            return false;
        }

        // Coarse-grained authorization by route group
        if (path.startsWith("/api/orders/")) {
            return role.equals("USER") || role.equals("SERVICE");
        }
        if (path.startsWith("/api/inventory/") || path.startsWith("/api/products/")) {
            return role.equals("USER") || role.equals("SERVICE");
        }

        return true;
    }

    public static class Config {
        // Configuration properties if needed
    }
}
