import os
import re

ROOT_DIR = r"f:\Gen_Projects\SmartTMS"

MODULES = [
    "analytics-service/analytics",
    "apigateway-service/apigateway",
    "auth-service/auth",
    "eureka-server/eureka-server",
    "order-service/order",
    "tracking-service/tracking",
    "user-service/user-service",
    "vehicle-service/vehicle-service",
    "ai-agent-service/ai-agent"
]

POM_DEPS_INJECTION = """
		<!-- Observability: OpenTelemetry & Micrometer -->
		<dependency>
			<groupId>io.micrometer</groupId>
			<artifactId>micrometer-tracing-bridge-otel</artifactId>
		</dependency>
		<dependency>
			<groupId>io.opentelemetry</groupId>
			<artifactId>opentelemetry-exporter-otlp</artifactId>
		</dependency>
		<dependency>
			<groupId>io.micrometer</groupId>
			<artifactId>micrometer-registry-prometheus</artifactId>
		</dependency>
		
		<!-- Centralized Logging: Loki -->
		<dependency>
			<groupId>com.github.loki4j</groupId>
			<artifactId>loki-logback-appender</artifactId>
			<version>1.5.2</version>
		</dependency>
		
		<!-- Config Server Client -->
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-config</artifactId>
		</dependency>
		
		<!-- API Docs / Swagger -->
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
			<version>2.6.0</version>
		</dependency>
"""

FEIGN_DEPS_INJECTION = """
		<!-- Resilience4j Circuit Breaker -->
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
		</dependency>
"""

LOGBACK_XML = """<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <springProperty scope="context" name="springAppName" source="spring.application.name"/>
    
    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
        <http>
            <url>http://localhost:3100/loki/api/v1/push</url>
        </http>
        <format>
            <label>
                <pattern>app=${springAppName},host=${HOSTNAME},traceID=%X{traceId:-NONE},level=%level</pattern>
            </label>
            <message>
                <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            </message>
            <sortByTime>true</sortByTime>
        </format>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="LOKI" />
    </root>
</configuration>
"""

APP_PROPS_INJECTION = """
# Observability / Tracing / Metrics
management.tracing.sampling.probability=1.0
management.endpoints.web.exposure.include=prometheus,health,info,metrics
management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
management.metrics.tags.application=${spring.application.name}

# Config Server Client
spring.config.import=optional:configserver:http://localhost:8888
"""

FEIGN_PROPS_INJECTION = """
# Resilience4j Circuit Breaker
spring.cloud.openfeign.circuitbreaker.enabled=true
resilience4j.circuitbreaker.instances.default.slidingWindowSize=10
resilience4j.circuitbreaker.instances.default.failureRateThreshold=50
resilience4j.circuitbreaker.instances.default.waitDurationInOpenState=10000ms
resilience4j.circuitbreaker.instances.default.permittedNumberOfCallsInHalfOpenState=3
"""

def update_pom(pom_path, is_feign=False):
    with open(pom_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if "micrometer-tracing-bridge-otel" in content:
        return # Already injected
    
    injection = POM_DEPS_INJECTION
    if is_feign:
        injection += FEIGN_DEPS_INJECTION
        
    # Find closing dependencies tag and inject right before it
    content = content.replace("</dependencies>", injection + "\t</dependencies>")
    
    with open(pom_path, 'w', encoding='utf-8') as f:
        f.write(content)

def update_properties(props_path, is_feign=False):
    if not os.path.exists(props_path):
        return
        
    with open(props_path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    if "management.tracing.sampling.probability" in content:
        return
        
    injection = APP_PROPS_INJECTION
    if is_feign:
        injection += FEIGN_PROPS_INJECTION
        
    with open(props_path, 'a', encoding='utf-8') as f:
        f.write("\n" + injection)

def has_feign_clients(module_path):
    src_main_java = os.path.join(module_path, "src", "main", "java")
    if not os.path.exists(src_main_java):
        return False
        
    for root, _, files in os.walk(src_main_java):
        for file in files:
            if file.endswith(".java"):
                with open(os.path.join(root, file), 'r', encoding='utf-8') as f:
                    content = f.read()
                    if "@FeignClient" in content:
                        return True
    return False

def main():
    for mod in MODULES:
        mod_path = os.path.join(ROOT_DIR, mod)
        if not os.path.exists(mod_path):
            continue
            
        is_feign = has_feign_clients(mod_path)
        
        # 1. Update pom.xml
        pom_path = os.path.join(mod_path, "pom.xml")
        if os.path.exists(pom_path):
            update_pom(pom_path, is_feign)
            print(f"Updated pom.xml for {mod}")
            
        # 2. Add logback-spring.xml
        resources_path = os.path.join(mod_path, "src", "main", "resources")
        if os.path.exists(resources_path):
            logback_path = os.path.join(resources_path, "logback-spring.xml")
            if not os.path.exists(logback_path):
                with open(logback_path, 'w', encoding='utf-8') as f:
                    f.write(LOGBACK_XML)
                print(f"Created logback-spring.xml for {mod}")
                
            # 3. Update application.properties
            props_path = os.path.join(resources_path, "application.properties")
            if os.path.exists(props_path):
                update_properties(props_path, is_feign)
                print(f"Updated application.properties for {mod}")

if __name__ == "__main__":
    main()
