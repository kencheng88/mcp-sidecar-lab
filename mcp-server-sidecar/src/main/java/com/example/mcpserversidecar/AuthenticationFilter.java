package com.example.mcpserversidecar;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Component
public class AuthenticationFilter implements WebFilter {

    public static final String AUTH_TOKEN_KEY = "X-MCP-Auth-Token";

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader != null) {
            return chain.filter(exchange)
                    .contextWrite(Context.of(AUTH_TOKEN_KEY, authHeader));
        }

        return chain.filter(exchange);
    }
}
