package com.smartlogistics.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserHeaderPropagationFilter implements GlobalFilter, Ordered {


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    String username = auth.getToken().getSubject();
                    Object userIdClaim = auth.getToken().getClaims().get("userId");
                    String userId = userIdClaim == null ? null : userIdClaim.toString();

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(builder -> {
                                builder.headers(headers -> {
                                    headers.set("X-User-Name", username);
                                    if (userId != null && !userId.isBlank()) {
                                        headers.set("X-User-Id", userId);
                                    }
                                });
                            })
                            .build();

                    return chain.filter(mutatedExchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
