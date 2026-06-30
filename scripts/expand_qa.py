import re
import sys

FILE_PATH = r"f:\Gen_Projects\SmartTMS\Docs\SmartTMS_100_Interview_Questions.md"

def expand_question(content, question_number, new_answer):
    # Regex to match the question and its answer until the next question or section header
    pattern = re.compile(rf"(\*\*{question_number}\..*?\*\*.*?)\*Answer\*:(.*?)(?=\n\*\*|\n## |\Z)", re.DOTALL)
    
    match = pattern.search(content)
    if match:
        question_header = match.group(1)
        # Replace the match with the new answer
        new_text = f"{question_header}\n*Answer*: {new_answer}\n"
        content = content[:match.start()] + new_text + content[match.end():]
        print(f"Expanded Q{question_number}")
    else:
        print(f"Could not find Q{question_number}")
    
    return content


EXPANSIONS = {
    9: """When the Driver App POSTs Lat/Lng to the API Gateway, the Gateway (Spring Cloud Gateway) intercepts the request in a Global WebFlux Filter. It validates the Bearer JWT, extracts the `driverId`, mutates the request to inject `X-User-Id` headers, and routes it to the Tracking Service. 
The Tracking Service performs a lightweight validation to ensure the driver is linked to an active vehicle. Instead of hitting a relational DB, it executes a Redis `SETEX` command with the key `live:vehicle:{vId}`, setting a 60-second TTL. Because Redis is an in-memory data store, this operation completes in <1ms. 
Simultaneously, it wraps the telemetry payload into a `VEHICLE_LOCATION_UPDATED` JSON string and uses `KafkaTemplate` to publish it to the `vehicle-location-events` topic, using the `vehicleId` as the partition key. The API returns a 200 OK immediately, ensuring high throughput. Downstream, the Analytics Service asynchronously polls this Kafka partition to calculate historical distance and persists it in MySQL using batch inserts.""",

    10: """The dispatch process spans multiple services and relies on the Choreography Saga pattern to maintain data consistency without Distributed Transactions (2PC).
1. The Dispatcher calls the Gateway which routes to Order Service. The Order Service creates the order in MySQL (`status: CREATED`) and asynchronously publishes an `ORDER_CREATED` event to the `order-events` Kafka topic.
2. The Dispatcher invokes the assignment endpoint (`/order/{id}/assign`). The Order Service makes a synchronous Feign Client call to the Vehicle Service (`/vehicle/route-match`).
3. The Vehicle Service queries its MySQL DB (or a geospatial Redis index) to find `IDLE` vehicles whose route coordinates intersect the order's bounds. It returns the best match.
4. The Order Service issues another Feign call to `/vehicle/{vId}/orders/{oId}` to bind the order.
5. Inside the Vehicle Service, a local database transaction updates the vehicle to `OCCUPIED`. Crucially, we use Optimistic Locking (`@Version` in JPA) here. If another dispatcher tried to assign the same vehicle concurrently, one transaction will throw an `ObjectOptimisticLockingFailureException` and fail cleanly. 
6. Upon success, Vehicle Service publishes `VEHICLE_STATUS_UPDATED`. The Order Service commits the `ASSIGNED` state and publishes `ORDER_ASSIGNED`.""",

    12: """Authentication is handled centrally at the API Gateway using a custom `GlobalFilter` in Spring Cloud Gateway. When a request arrives, the filter intercepts it on the Netty reactor thread. It extracts the `Authorization: Bearer` header. 
Instead of making an expensive network call to the Auth Service for every request, the Gateway validates the JWT's RSA signature using a locally cached public key. It checks the `exp` (expiration) claim and verifies the token's `jti` (JWT ID) against a Redis Blacklist (`gateway:jwt:blacklist`). 
If valid, it parses the claims to extract the `userId` and `role`. It then mutates the `ServerHttpRequest` to inject custom headers (e.g., `X-Auth-User-Id`, `X-Auth-User-Role`) so downstream microservices don't have to parse the JWT themselves. If invalid, the Gateway immediately terminates the WebFlux chain and returns a `401 Unauthorized` without burdening the downstream services.""",

    24: """JWTs are intrinsically stateless, meaning all necessary data (claims, expiration, signature) is encoded within the token itself. The Gateway validates the JWT by verifying its cryptographic signature (e.g., HMAC-SHA256 or RSA). If the signature is valid, it proves the token was issued by our Auth Service and hasn't been tampered with. 
However, to handle edge cases like forced logouts or compromised accounts, we implement a "Cache-Aside Blacklist" in Redis. When a token is revoked, its unique ID (`jti`) is added to Redis with a TTL matching the token's remaining lifespan. During validation, the Gateway does a quick O(1) Redis `EXISTS` check on the `jti`. This hybrid approach combines the performance of stateless validation with the security of stateful revocation.""",

    49: """We solve the concurrency issue using Optimistic Locking, a standard pattern in high-concurrency relational databases. In our JPA Entity for `Vehicle`, we include a `@Version` field. 
When Dispatcher A and Dispatcher B concurrently fetch Vehicle 101, they both read version `v=1`. Dispatcher A's transaction attempts to update the vehicle to `OCCUPIED`. Hibernate generates the SQL: `UPDATE vehicle SET status='OCCUPIED', version=2 WHERE id=101 AND version=1`. This succeeds.
A millisecond later, Dispatcher B's transaction attempts the same update. However, the database version is now 2. Dispatcher B's SQL (`WHERE id=101 AND version=1`) affects 0 rows. Hibernate detects this and throws an `ObjectOptimisticLockingFailureException`. The system rolls back Dispatcher B's transaction and returns an error (or triggers a retry), guaranteeing that a vehicle cannot be double-assigned.""",

    55: """To synchronize MySQL (our ACID Source of Truth) with Elasticsearch (our fast query engine) without introducing brittle distributed transactions, we utilize Event-Driven Data Synchronization, specifically leaning toward the Outbox Pattern or Change Data Capture (CDC).
When the Order Service updates a record in MySQL, it publishes an event (`ORDER_UPDATED`) to Kafka. An independent worker (or Logstash/Kafka Connect) subscribes to this topic. It reads the updated order payload and upserts the corresponding document in the Elasticsearch index. 
This guarantees Eventual Consistency. If Elasticsearch is temporarily down, Kafka retains the events. Once ES recovers, the consumer picks up from its last committed offset and processes the backlog, ensuring zero data loss and removing any synchronous coupling between MySQL and Elasticsearch.""",

    65: """The key `live:vehicle:{vehicleId}` holds a JSON string of the latest telemetry. The 30-second TTL (Time-To-Live) is a critical architectural decision. 
Instead of running expensive background cron jobs to detect "stale" or offline vehicles, we rely on Redis's native expiration mechanism. Every time a GPS ping arrives, the `SETEX` command overwrites the data and resets the 30s TTL. If a vehicle drives into a tunnel or the app crashes, no new pings arrive. After 30 seconds, Redis automatically evicts the key.
When the Dispatcher UI polls for live vehicles, it only queries existing keys. Thus, offline vehicles seamlessly disappear from the active map, eliminating "ghost tracking" without any manual database cleanup logic.""",

    68: """Kafka partitions topics to distribute load. However, order matters in GPS tracking—if ping A (timestamp 10:00:01) is processed after ping B (timestamp 10:00:05), the analytics service might calculate a negative distance or erratic path.
By specifying the `vehicleId` as the Kafka message key, Kafka uses a hashing algorithm (murmur2) on the key to assign the message to a specific partition (e.g., `hash(vehicleId) % numPartitions`). 
Because all messages for `vehicle_123` land in Partition 3, and a single Kafka partition is consumed by only one consumer thread within a consumer group, strict chronological processing is guaranteed for that vehicle. Other vehicles will hash to different partitions, allowing horizontal scalability across multiple consumer instances.""",

    77: """Kafka guarantees "at-least-once" delivery, meaning a network blip might cause Kafka to deliver the same event twice. To prevent double-counting mileage or corrupting states, our consumers must be idempotent.
For location events, the Analytics consumer extracts the `timestamp` and `vehicleId` from the payload. The MySQL `vehicle_location_history` table has a composite UNIQUE constraint on `(vehicle_id, timestamp)`. 
When the consumer tries to insert a duplicate event, MySQL throws a `DataIntegrityViolationException`. The consumer catches this, ignores the duplicate, and safely commits the Kafka offset. For order state changes, we check the current state before applying a transition (e.g., if an order is already `DELIVERED`, ignore a duplicate `ORDER_DELIVERED` event).""",

    95: """In a microservices architecture, traditional 2-Phase Commit (2PC) distributed transactions are anti-patterns because they create synchronous blocking and tight coupling. We use the **Saga Pattern** based on Choreography.
A Saga breaks a distributed transaction into a sequence of local database transactions. If step 1 succeeds, it publishes an event triggering step 2. 
If step 2 fails (e.g., Vehicle Service cannot assign the driver), the Vehicle Service publishes a `VEHICLE_ASSIGNMENT_FAILED` event. The Order Service listens for this failure event and executes a **Compensating Transaction**—it updates the order status in its local database back from `PENDING_ASSIGNMENT` to `CREATED`. This model embraces Eventual Consistency while maintaining high availability and resilience.""",

    98: """We use **OpenTelemetry (OTel)** integrated via **Micrometer Tracing**. When a request hits the API Gateway, Micrometer automatically intercepts it and generates a unique `traceId` and `spanId`.
It injects these IDs into standard HTTP headers (like W3C `traceparent` or B3 headers) before routing the request to downstream services. Every subsequent service extracts the `traceId` from the headers, continues the trace, and generates a new `spanId` for its local execution.
Concurrently, the OTLP Exporter asynchronously batches and sends these trace trees to **Grafana Tempo**. Because our Logback configuration (`loki-logback-appender`) natively embeds this same `traceId` into every log line pushed to **Loki**, a developer can open a Grafana dashboard, search for an error log, and click the `traceId` to instantly visualize the entire network hop topology and pinpoint exactly which microservice caused the bottleneck."""
}


def main():
    with open(FILE_PATH, 'r', encoding='utf-8') as f:
        content = f.read()

    for q_num, new_ans in EXPANSIONS.items():
        content = expand_question(content, q_num, new_ans)

    with open(FILE_PATH, 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    main()
