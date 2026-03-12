package com.smartlogistics.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Component
public class UserHeaderPropagationFilter implements GlobalFilter, Ordered {


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx->ctx.getAuthentication())
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth->{
                    String username = auth.getToken().getSubject();
                    String userId = auth.getToken().getClaim("userId");
                    String role = auth.getToken().getClaim("role");
                    String email = auth.getToken().getClaim("email");
                    ServerHttpRequest mutatedRequest = (ServerHttpRequest) exchange.getRequest()
                            .mutate()
                            .header("X-User-Name", username)
                            .header("X-User-Id", userId)
                            .header("X-User-Role", role)
                            .header("X-User-Email", email)
                            .build();
                    return chain.filter(exchange.mutate().request((Consumer<org.springframework.http.server.reactive.ServerHttpRequest.Builder>) mutatedRequest).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
