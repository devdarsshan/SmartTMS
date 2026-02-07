
---

# PHASE 0 – FOUNDATION & INFRASTRUCTURE

---

## 🎯 Phase-0 Goal (Read This First)

By the end of Phase-0:

* All Spring Boot services are running independently
* All services are registered in **Eureka**
* All client traffic goes through **API Gateway**
* All services can connect to **one shared MySQL database**
* Health checks work for every service

⚠️ **No business logic in this phase. Only infrastructure.**

---

## 🧩 Components You Will Work On

You will work on **these Spring Boot projects**:

1. Eureka Server
2. API Gateway
3. Auth Service
4. User Service
5. Vehicle Service
6. Order Service
7. Tracking Service
8. Analytics Service

Each is a **separate Spring Boot project**.

---

## STEP 1: Eureka Server (Service Discovery)

### Why this exists

Eureka is like a **phonebook**.
Services register here so others can find them dynamically.

---

### What to create

**Project name**

```
eureka-server
```

### Dependencies (Maven/Gradle)

* Spring Web
* Eureka Server

---

### Main class

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

---

### application.yml

```yaml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

---

### How to verify

1. Run the app
2. Open browser → `http://localhost:8761`
3. You should see Eureka dashboard

If this fails → STOP and fix before moving on.

---

## STEP 2: API Gateway (Single Entry Point)

### Why this exists

* Clients should **never call services directly**
* Gateway routes requests
* Gateway later handles security & rate limiting

---

### Project

```
api-gateway
```

### Dependencies

* Spring Cloud Gateway
* Eureka Client
* Spring Boot Actuator

---

### Main class

```java
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

---

### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway

  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```

---

### What this config does (important)

* Gateway automatically discovers services from Eureka
* Routes requests by service name
* No hardcoded URLs

---

### Verify

1. Start Eureka
2. Start Gateway
3. Gateway should appear in Eureka dashboard

---

## STEP 3: Base Setup for ALL Microservices

This step applies to **Auth, User, Vehicle, Order, Tracking, Analytics services**.

---

### Required dependencies (EVERY service)

* Spring Web
* Eureka Client
* Spring Boot Actuator

---

### Main class template

```java
@SpringBootApplication
@EnableDiscoveryClient
public class <ServiceName>Application {
    public static void main(String[] args) {
        SpringApplication.run(<ServiceName>Application.class, args);
    }
}
```

---

### application.yml (Base)

```yaml
server:
  port: 0   # random port to avoid conflicts

spring:
  application:
    name: <service-name>

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```

---

### Service naming convention (IMPORTANT)

| Service   | application.name  |
| --------- | ----------------- |
| Auth      | auth-service      |
| User      | user-service      |
| Vehicle   | vehicle-service   |
| Order     | order-service     |
| Tracking  | tracking-service  |
| Analytics | analytics-service |

This name is what Gateway uses for routing.

---

### Verify EACH service

For every service:

1. Start Eureka
2. Start the service
3. Check Eureka dashboard
4. Service should appear with random port

If even ONE service fails → fix before proceeding.

---

## STEP 4: Shared MySQL Database Setup

### Why single DB?

* Simpler setup
* Faster development
* Easier debugging

We’ll split tables logically per service.

---

### MySQL Setup

Create DB:

```sql
CREATE DATABASE logistics_db;
```

---

### Dependencies (each service that uses DB)

* Spring Data JPA
* MySQL Connector

---

### application.yml (DB config)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/logistics_db
    username: root
    password: root

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

---

### Table strategy

* Prefix tables per service

Examples:

* `usr_users`
* `veh_vehicles`
* `ord_orders`
* `trk_location_history`

This avoids confusion later.

---

### Verify DB connection

* Start service
* No DB errors in logs
* Tables auto-created

---

## STEP 5: Actuator & Health Checks

### Why?

* Gateway & ops teams need to know service health
* Eureka uses health checks

---

### application.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

---

### Verify

Open in browser:

```
http://localhost:<service-port>/actuator/health
```

Expected:

```json
{ "status": "UP" }
```

---

## STEP 6: End-to-End Infrastructure Validation

### Final checklist (MANDATORY)

Before Phase-1, confirm:

✅ Eureka Server running
✅ API Gateway registered
✅ All services registered
✅ All services use random ports
✅ MySQL accessible
✅ Actuator health is UP

If anything fails → DO NOT move forward.

---

## How This Phase Is Explained in Interviews

You should be able to say:

> “We first established the infrastructure layer using Eureka for service discovery and Spring Cloud Gateway as a centralized entry point, ensuring all services were discoverable, scalable, and independently deployable.”
