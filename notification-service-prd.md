# Product Requirements Document: Notification Service

## 1. Purpose

This document defines the backend-only product and engineering requirements for a future `notification-service` microservice in SmartTMS.

This service is not being implemented now. The purpose of this PRD is to define:

- what the service should do
- how it should integrate with the current services
- what event contracts existing services must expose when this service is added later

---

## 2. Why a Separate Notification Microservice

SmartTMS already has multiple business flows that should trigger notifications, but notification logic is not present in the current codebase.

A separate `notification-service` is required so that:

- email delivery is centralized
- future in-app notifications are centralized
- business services do not embed notification provider logic
- notifications can be retried, audited, and scaled independently

This service should exist as a separate Spring Boot microservice, just like:

- `order-service`
- `vehicle-service`
- `tracking-service`
- `analytics-service`

---

## 3. Scope

## 3.1 Phase 1 Scope

- separate `notification-service` Spring Boot service
- Eureka registration
- API Gateway route
- Kafka consumers for business events
- email notification delivery
- notification persistence in MySQL
- support for internal APIs if needed later

## 3.2 Out of Scope for Now

- UI implementation
- push notifications
- SMS
- WhatsApp
- notification preference center

---

## 4. Channels in Scope

Per product decision, phase 1 channels are:

- `EMAIL`
- `IN_APP`

Important implementation note:

- even if no UI is built now, `IN_APP` notifications should still be persisted in the service data model so a future backend consumer or frontend can use them later

---

## 5. Target Architecture

```text
Order Service ----\
Vehicle Service ---+----> Kafka ----> Notification Service ----> MySQL
Tracking Service --/                              |
                                                  +----> Email Provider
```

Integration style for phase 1:

- event-driven first
- direct service-to-service API optional later for exceptional cases

Default rule:

- business state changes should publish events
- `notification-service` should consume those events and decide recipients, channels, template, and delivery

---

## 6. Required Platform Integration

## 6.1 New Microservice

A new service directory should be added later, for example:

```text
notification-service/notification
```

It should follow the same conventions as the existing services:

- Spring Boot
- Eureka client
- `application.properties`
- controller/service/repository layers

## 6.2 API Gateway

When the service is created, gateway must add:

```text
/notifications/** -> notification-service
```

## 6.3 Eureka

The service must register with Eureka and be discoverable like the other services.

## 6.4 Security

The service must:

- rely on gateway JWT validation for external APIs
- protect internal-only endpoints
- use propagated `X-User-Id` where a user is querying their notifications

---

## 7. Responsibilities of Notification Service

- consume business events from Kafka
- determine recipients for each event type
- create notification records
- send email notifications
- persist in-app notifications
- expose APIs to query notifications later
- track delivery status and failures
- support retries for transient email failures

This service must not:

- own order state
- own vehicle state
- own user profile state
- become the source of truth for operational business data

---

## 8. Data Model

## 8.1 Notifications Table

### `notifications`

| column | type |
|--------|------|
| notification_id | BIGINT |
| recipient_user_id | BIGINT |
| recipient_role | VARCHAR |
| channel | VARCHAR |
| type | VARCHAR |
| title | VARCHAR |
| message | TEXT |
| entity_type | VARCHAR |
| entity_id | BIGINT |
| status | VARCHAR |
| is_read | BOOLEAN |
| created_at | TIMESTAMP |
| sent_at | TIMESTAMP |
| read_at | TIMESTAMP |
| metadata_json | JSON/TEXT |

## 8.2 Recommended Enums

### Channel

- `EMAIL`
- `IN_APP`

### Delivery Status

- `PENDING`
- `SENT`
- `FAILED`

### Notification Type

- `ORDER_CREATED`
- `ORDER_ASSIGNED`
- `ORDER_IN_TRANSIT`
- `ORDER_DELIVERED`
- `TRANSPORT_ORDER_CREATED`
- `DRIVER_ASSIGNED_TO_VEHICLE`
- `DRIVER_UNASSIGNED_FROM_VEHICLE`
- `VEHICLE_STATUS_UPDATED`
- `TRACKING_STALE`

---

## 9. APIs for Future Implementation

These APIs should be implemented when the service is built later.

### GET `/notifications/me`

Purpose:

- fetch notifications for the authenticated user

### GET `/notifications/me/unread-count`

Purpose:

- fetch unread notification count

### PUT `/notifications/{notificationId}/read`

Purpose:

- mark a notification as read

### POST `/notifications/internal/send`

Purpose:

- internal-only direct send API for exceptional synchronous use cases

Important note:

- Kafka consumption should remain the default and preferred model

---

## 10. Notification Integration Points in Existing Services

This section identifies where the future `notification-service` should integrate with the current codebase.

## 10.1 Order Service Integration

### A. Order Created

Current source in code:

- `order-service.createOrder(...)`

Future behavior:

- after successful order creation, emit `ORDER_CREATED`

Recipients:

- creator
- dispatcher group for customer orders

Suggested channel use:

- email to creator
- in-app for dispatchers

### B. Order Assigned

Current source in code:

- `order-service.assignOrder(...)`

Future behavior:

- after successful vehicle assignment and status move to `ASSIGNED`, emit `ORDER_ASSIGNED`

Recipients:

- order creator
- assigned driver, if vehicle has a driver
- dispatcher

### C. Order In Transit

Current source in code:

- `order-service.updateOrderStatus(... IN_TRANSIT ...)`

Future behavior:

- emit `ORDER_IN_TRANSIT`

Recipients:

- order creator
- dispatcher

### D. Order Delivered

Current source in code:

- `order-service.updateOrderStatus(... DELIVERED ...)`

Future behavior:

- emit `ORDER_DELIVERED`

Recipients:

- order creator
- dispatcher

## 10.2 Vehicle Service Integration

### E. Driver Assigned to Vehicle

Current source in code:

- `vehicle-service.assignDriverToVehicle(...)`

Future behavior:

- emit `DRIVER_ASSIGNED_TO_VEHICLE`

Recipients:

- assigned driver
- dispatcher

### F. Driver Unassigned From Vehicle

Current source in code:

- `vehicle-service.unassignDriverFromVehicle(...)`

Future behavior:

- emit `DRIVER_UNASSIGNED_FROM_VEHICLE`

Recipients:

- driver
- dispatcher

### G. Vehicle Status Changed

Current source in code:

- `vehicle-service.updateVehicleStatus(...)`

Future behavior:

- emit `VEHICLE_STATUS_UPDATED`

Primary recipients:

- dispatcher
- admin in selected operational states such as `MAINTENANCE` and `DISCARDED`

## 10.3 Tracking Service Integration

### H. Tracking Stale

Future source:

- `tracking-service` scheduled monitor or rule-based processor

Trigger:

- no location update for a threshold while vehicle is in transit or linked to active orders

Recipients:

- dispatcher
- admin

### I. Delay or Route Deviation

Future source:

- `analytics-service` or later rule engine

Recipients:

- dispatcher
- admin

This is best treated as phase 2.

## 10.4 User and Auth Service Integration

### J. User Registered

Current source:

- `auth-service.register(...)`

Possible later use:

- welcome email

### K. User Profile Created

Current source:

- `user-service.createUserProfile(...)`

Possible later use:

- profile completion confirmation

These are optional and should not block core logistics notification flows.

---

## 11. Required Event Contracts for Future Integration

To integrate `notification-service` later, the existing services should emit stable Kafka events.

## 11.1 Order Events

### `ORDER_CREATED`

```json
{
  "eventId": "uuid",
  "eventType": "ORDER_CREATED",
  "occurredAt": "2026-04-11T15:00:00Z",
  "orderId": 501,
  "orderType": "CUSTOMER_ORDER",
  "createdByUserId": 44,
  "createdByRole": "USER",
  "from": "Bangalore",
  "to": "Chennai",
  "status": "CREATED"
}
```

### `ORDER_ASSIGNED`

```json
{
  "eventId": "uuid",
  "eventType": "ORDER_ASSIGNED",
  "occurredAt": "2026-04-11T15:10:00Z",
  "orderId": 501,
  "vehicleId": 101,
  "driverId": 12,
  "status": "ASSIGNED"
}
```

### `ORDER_IN_TRANSIT`

```json
{
  "eventId": "uuid",
  "eventType": "ORDER_IN_TRANSIT",
  "occurredAt": "2026-04-11T15:30:00Z",
  "orderId": 501,
  "vehicleId": 101,
  "status": "IN_TRANSIT"
}
```

### `ORDER_DELIVERED`

```json
{
  "eventId": "uuid",
  "eventType": "ORDER_DELIVERED",
  "occurredAt": "2026-04-11T18:00:00Z",
  "orderId": 501,
  "vehicleId": 101,
  "status": "DELIVERED",
  "deliveredAt": "2026-04-11T18:00:00Z"
}
```

## 11.2 Vehicle Events

### `DRIVER_ASSIGNED_TO_VEHICLE`

```json
{
  "eventId": "uuid",
  "eventType": "DRIVER_ASSIGNED_TO_VEHICLE",
  "occurredAt": "2026-04-11T12:00:00Z",
  "vehicleId": 101,
  "driverId": 12
}
```

### `DRIVER_UNASSIGNED_FROM_VEHICLE`

```json
{
  "eventId": "uuid",
  "eventType": "DRIVER_UNASSIGNED_FROM_VEHICLE",
  "occurredAt": "2026-04-11T19:00:00Z",
  "vehicleId": 101,
  "driverId": 12
}
```

### `VEHICLE_STATUS_UPDATED`

```json
{
  "eventId": "uuid",
  "eventType": "VEHICLE_STATUS_UPDATED",
  "occurredAt": "2026-04-11T15:30:00Z",
  "vehicleId": 101,
  "vehicleStatus": "IN_TRANSIT"
}
```

## 11.3 Tracking Events

### `TRACKING_STALE`

```json
{
  "eventId": "uuid",
  "eventType": "TRACKING_STALE",
  "occurredAt": "2026-04-11T16:00:00Z",
  "vehicleId": 101,
  "driverId": 12,
  "activeOrderIds": [501],
  "lastLocationAt": "2026-04-11T15:20:00Z"
}
```

---

## 12. Recipient Resolution Rules

The future service should resolve recipients using identifiers already present in operational services.

### Examples

- order creator:
  - from `order-service` event payload
- driver:
  - from `vehicle-service` assignment state or event payload
- dispatcher group:
  - query `user-service` by role `DISPATCHER`
- admin group:
  - query `user-service` by role `ADMIN`

The notification service should not become the master source of user role mappings.

---

## 13. Delivery Rules

## 13.1 Email

- email should be asynchronous
- transient failures should be retried
- permanent failures should be marked `FAILED`

## 13.2 In-App

- in-app notifications should always be persisted when requested
- future clients may query them from the notification-service APIs

## 13.3 Idempotency

- duplicate Kafka events must not create duplicate notifications without intent
- use `eventId + recipient + channel + type` as a deduplication strategy

---

## 14. Non-Functional Requirements

### Reliability

- consumer processing must be idempotent
- notification persistence must survive email delivery failures

### Security

- user-facing endpoints must require authentication
- internal-only send endpoint must not be publicly accessible
- PII included in Kafka payloads should be minimal

### Observability

- every notification should be traceable to source event id
- delivery failures and retries must be logged

---

## 15. Implementation Readiness Checklist for Later

When you decide to build this service later, these changes should be done:

1. Create `notification-service` as a new Spring Boot microservice.
2. Add Eureka registration and gateway route.
3. Add MySQL schema for notification persistence.
4. Add Kafka consumers for order, vehicle, and tracking events.
5. Update existing services to publish stable events if not already implemented.
6. Add email provider integration.
7. Add query APIs for notification retrieval.

---

## 16. Final Summary

This PRD defines `notification-service` as a separate future microservice, not as logic embedded inside tracking, analytics, or any existing service.

Its purpose is to integrate later with the existing SmartTMS services through Kafka events and minimal direct coupling.

When implemented, it should:

- consume business events
- send emails
- persist in-app notifications
- stay independent from operational business state
