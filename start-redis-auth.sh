#!/bin/bash

echo "=========================================="
echo "SmartTMS Auth Service - Redis Setup Guide"
echo "=========================================="
echo

echo "1. Starting Redis with Docker..."
docker-compose -f docker-compose.redis.yml up -d

echo
echo "2. Waiting for Redis to start..."
sleep 5

echo
echo "3. Checking Redis connection..."
docker exec smarttms-redis redis-cli -a smarttms123 ping

echo
echo "4. Redis Commander Web UI: http://localhost:8081"
echo "5. Redis Server: localhost:6379"
echo "6. Redis Password: smarttms123"

echo
echo "=========================================="
echo "Redis Caching Features Implemented:"
echo "=========================================="
echo "✓ JWT Token Blacklisting"
echo "✓ User Authentication Caching"
echo "✓ Distributed Rate Limiting"
echo "✓ Session Management"
echo

echo "=========================================="
echo "Available Cache Management Endpoints:"
echo "=========================================="
echo "GET  /auth/cache/health         - Redis health check"
echo "POST /auth/logout               - Blacklist JWT token"
echo "GET  /auth/validate?token=XXX   - Validate JWT token"
echo "DEL  /auth/cache/user/{username} - Clear user cache"
echo "DEL  /auth/cache/all            - Clear all auth cache"
echo

echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo "1. Build auth-service: mvn clean compile"
echo "2. Run auth-service: mvn spring-boot:run"
echo "3. Test Redis integration with cache endpoints"
echo "4. Monitor Redis with Redis Commander"
echo

echo "Redis setup complete! 🚀"
