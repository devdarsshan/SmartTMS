package com.smartlogistics.apigateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class SwaggerConfig {

    private final RouteDefinitionLocator locator;
    private final SwaggerUiConfigParameters swaggerUiParameters;

    public SwaggerConfig(RouteDefinitionLocator locator, SwaggerUiConfigParameters swaggerUiParameters) {
        this.locator = locator;
        this.swaggerUiParameters = swaggerUiParameters;
    }

    @PostConstruct
    public void init() {
        Set<String> groupedUrls = new HashSet<>();
        locator.getRouteDefinitions().subscribe(routeDefinition -> {
            String resourceName = routeDefinition.getId();
            if (!groupedUrls.contains(resourceName)) {
                String url = "/" + resourceName + "/v3/api-docs";
                swaggerUiParameters.addGroup(resourceName, resourceName, url);
                groupedUrls.add(resourceName);
            }
        });
    }
}
