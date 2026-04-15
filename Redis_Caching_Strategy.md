# Redis Caching Strategy for SmartTMS

This document outlines where and how to implement Redis caching across the SmartTMS microservices to improve performance, reduce database load, and enhance user experience.

## Overview

Redis will be implemented as a distributed cache across services to:
- Cache frequently accessed data
- Reduce database queries
- Improve response times
- Enable session management
- Support real-time features

## Service-Specific Caching Strategy

### 1. Auth Service

**Cache Implementation Areas:**
- **JWT Blacklist**: Store revoked/blacklisted JWT tokens
- **User Authentication**: Cache user credentials for validation
- **Rate Limiting Data**: Store rate limiting counters per user/IP

**Redis Keys Pattern:**
```
auth:blacklist:{jti}
auth:user:{userId}
auth:rate_limit:{clientIp}:{endpoint}
```

**Benefits:** 
- Fast JWT validation without database hits
- Efficient rate limiting
- Quick user authentication checks

### 2. User Service

**Cache Implementation Areas:**
- **User Profiles**: Cache complete user profile data
- **Role-Based Queries**: Cache users by role (drivers, dispatchers)
- **User Role Validation**: Cache role information for quick access

**Redis Keys Pattern:**
```
user:profile:{userId}
user:role:{roleName}:list
user:role_check:{userId}
```

**Cache Duration:**
- User profiles: 1 hour (frequently updated)
- Role lists: 30 minutes (updated during business operations)

**Benefits:**
- Faster profile retrieval for order assignments
- Quick role validation for authorization
- Reduced database load for user lookups

### 3. Vehicle Service

**Cache Implementation Areas:**
- **Available Vehicles**: Cache idle vehicles by location and type
- **Vehicle Details**: Cache full vehicle information
- **Route Information**: Cache vehicle routes and through points
- **Driver Assignments**: Cache driver-vehicle relationships

**Redis Keys Pattern:**
```
vehicle:idle:{city}:{vehicleType}
vehicle:details:{vehicleId}
vehicle:route:{vehicleId}
vehicle:driver_assignment:{driverId}
```

**Cache Duration:**
- Idle vehicles: 5 minutes (high frequency updates)
- Vehicle details: 2 hours (stable data)
- Routes: 1 hour (periodic updates)

**Benefits:**
- Lightning-fast vehicle availability checks
- Reduced load on vehicle assignment queries
- Better performance for route planning

### 4. Order Service

**Cache Implementation Areas:**
- **Active Orders**: Cache in-progress orders by status
- **Order History**: Cache recent order history per user
- **Order-Vehicle Assignments**: Cache current assignments
- **Delivery Routes**: Cache optimized delivery routes

**Redis Keys Pattern:**
```
order:status:{status}:list
order:history:{userId}
order:assignment:{vehicleId}
order:route:{orderId}
```

**Cache Duration:**
- Active orders: 10 minutes (frequent status changes)
- Order history: 2 hours (historical data)
- Assignments: 30 minutes (operational changes)

**Benefits:**
- Fast order status queries
- Quick assignment lookups
- Improved dashboard performance

### 5. API Gateway

**Cache Implementation Areas:**
- **JWT Token Validation**: Cache valid JWT tokens
- **Rate Limiting**: Distributed rate limiting across gateway instances
- **Route Caching**: Cache routing decisions
- **User Session Data**: Cache user session information

**Redis Keys Pattern:**
```
gateway:jwt:{token_hash}
gateway:rate_limit:{clientIp}:{minute}
gateway:route_cache:{path}
gateway:session:{sessionId}
```

**Benefits:**
- Distributed rate limiting
- Reduced auth service calls
- Session management across instances

### 6. Analytics Service

**Cache Implementation Areas:**
- **Dashboard Metrics**: Cache computed analytics data
- **Report Data**: Cache frequently requested reports
- **Real-time Statistics**: Cache live operational metrics

**Redis Keys Pattern:**
```
analytics:dashboard:{timeframe}
analytics:report:{reportType}:{date}
analytics:realtime:metrics
```

**Cache Duration:**
- Dashboard data: 15 minutes
- Reports: 1 hour
- Real-time metrics: 2 minutes

### 7. Tracking Service

**Cache Implementation Areas:**
- **Vehicle Locations**: Cache current vehicle positions
- **Delivery Status**: Cache real-time delivery updates
- **Route Progress**: Cache delivery progress information

**Redis Keys Pattern:**
```
tracking:location:{vehicleId}
tracking:delivery:{orderId}
tracking:progress:{vehicleId}:{orderId}
```

**Cache Duration:**
- Vehicle locations: 30 seconds (real-time updates)
- Delivery status: 2 minutes
- Route progress: 1 minute

## Implementation Steps

### Step 1: Add Redis Dependencies

Add to each service's `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

### Step 2: Redis Configuration

Create `RedisConfig.java` in each service:
```java
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("localhost", 6379));
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration(Duration.ofMinutes(10)));
        return builder.build();
    }
}
```

### Step 3: Service Implementation

Add caching annotations to service methods:
```java
@Cacheable(value = "userProfiles", key = "#userId")
public UserProfile getUserProfile(Long userId) {
    return userRepository.findById(userId);
}

@CacheEvict(value = "userProfiles", key = "#userProfile.userId")
public UserProfile updateUserProfile(UserProfile userProfile) {
    return userRepository.save(userProfile);
}
```

### Step 4: Environment Configuration

Add to `application.properties`:
```properties
# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-wait=-1ms

# Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
```

## Cache Invalidation Strategy

### Event-Driven Cache Invalidation
- Use Spring Events to invalidate related caches
- Implement cache invalidation on entity updates
- Use Redis pub/sub for cross-service cache invalidation

### Time-Based Expiration
- Set appropriate TTL for different data types
- Use shorter TTL for frequently changing data
- Longer TTL for stable reference data

## Monitoring and Performance

### Redis Monitoring
- Monitor cache hit/miss ratios
- Track memory usage
- Monitor connection pool metrics
- Set up alerts for Redis availability

### Performance Metrics
- Measure response time improvements
- Track database query reduction
- Monitor cache effectiveness per service

## Security Considerations

- Use Redis AUTH for production
- Enable SSL/TLS for Redis connections
- Implement proper key isolation between services
- Regular security updates for Redis

## Best Practices

1. **Key Naming**: Use consistent, hierarchical key naming
2. **Serialization**: Use efficient serialization (Jackson/Kryo)
3. **Connection Pooling**: Configure proper connection pools
4. **Error Handling**: Implement fallback mechanisms
5. **Testing**: Include cache testing in unit/integration tests

## Redis Deployment with Docker

### Quick Start with Docker

**Start Redis using Docker Compose:**
```bash
docker compose -f docker-compose.redis.yml --env-file .env.redis up -d
```

This will start:
- **Redis Server**: Available at `localhost:6379`
- **Redis Commander**: Web UI at `http://localhost:8081`

**Stop Redis:**
```bash
docker compose -f docker-compose.redis.yml --env-file .env.redis down
```

**View Redis logs:**
```bash
docker logs smarttms-redis
```

### Environment Configuration for Docker

Update your service `application.properties` to use Docker Redis:
```properties
# Redis Configuration for Docker
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=smarttms123
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-wait=-1ms

# Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
```

### Production Considerations

For production deployment:
1. **Create `.env.redis` from `.env.redis.example`** and set a strong `REDIS_PASSWORD`
2. **Enable SSL/TLS** for Redis connections
3. **Use Redis Sentinel** or **Redis Cluster** for high availability
4. **Configure proper backup** strategies
5. **Set up monitoring** with Redis metrics

## Deployment Recommendations

- **Development**: Single Redis instance via Docker Compose
- **Production**: Redis Cluster or Redis Sentinel for high availability
- **Scaling**: Consider Redis sharding for large datasets
- **Backup**: Implement Redis persistence and backup strategies

## Getting Started

1. **Start Redis**: `docker compose -f docker-compose.redis.yml --env-file .env.redis up -d`
   - First create `.env.redis` from `.env.redis.example`
2. **Verify Redis**: Access Redis Commander at `http://localhost:8081`
3. **Add dependencies** to your services (see Step 1 above)
4. **Configure Redis** in each service (see Step 2-4 above)
5. **Test caching** functionality

This caching strategy will significantly improve the SmartTMS performance while maintaining data consistency and system reliability.
