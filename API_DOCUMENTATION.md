# SmartTMS API Documentation

This document contains all API endpoints, HTTP methods, and payloads for testing the SmartTMS application.

## Base URLs

- **API Gateway**: `http://localhost:7082`
- **Auth Service**: `http://localhost:7083`
- **Order Service**: `http://localhost:7084`
- **User Service**: `http://localhost:7086`
- **Vehicle Service**: `http://localhost:7087`

**Note**: All requests should be made through the API Gateway (`http://localhost:7082`). The gateway routes requests to the appropriate microservices.

---

## 1. Authentication Service

### 1.1 Register User
**Endpoint**: `POST /auth/register`  
**Gateway URL**: `http://localhost:7082/auth/register`  
**Description**: Register a new user account

**Request Body**:
```json
{
  "username": "johndoe123",
  "password": "Test@1234"
}
```

**Validation Rules**:
- `username`: Required, 3-50 characters, alphanumeric with underscores and hyphens only
- `password`: Required, minimum 8 characters, must contain at least one uppercase letter, one lowercase letter, one digit, and one special character (@$!%*?&)

**Response**: `201 Created` (No body)

**Example cURL**:
```bash
curl -X POST http://localhost:7082/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"johndoe123","password":"Test@1234"}'
```

---

### 1.2 Login User
**Endpoint**: `POST /auth/login`  
**Gateway URL**: `http://localhost:7082/auth/login`  
**Description**: Authenticate user and receive JWT token

**Request Body**:
```json
{
  "username": "johndoe123",
  "password": "Test@1234"
}
```

**Response**: `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1
}
```

**Example cURL**:
```bash
curl -X POST http://localhost:7082/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"johndoe123","password":"Test@1234"}'
```

---

## 2. User Service

### 2.1 Create User Profile
**Endpoint**: `POST /user/createProfile`  
**Gateway URL**: `http://localhost:7082/user/createProfile`  
**Description**: Create a user profile after registration

**Headers**:
- `X-User-Id`: User ID from login response (Required)
- `Content-Type`: application/json

**Request Body**:
```json
{
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "userRole": "USER",
  "city": "New York",
  "driverStatus": "ACTIVE"
}
```

**Field Details**:
- `email`: Required, valid email format
- `firstName`: Required
- `lastName`: Optional
- `phoneNumber`: Optional
- `userRole`: Required, one of: `ADMIN`, `USER`, `DRIVER`, `DISPATCHER`
- `city`: Optional
- `driverStatus`: Optional (for drivers), one of: `ACTIVE`, `OCCUPIED`, `INACTIVE`

**Response**: `200 OK` (No body)

**Example cURL**:
```bash
curl -X POST http://localhost:7082/user/createProfile \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890",
    "userRole": "USER",
    "city": "New York"
  }'
```

---

### 2.2 Update User Profile
**Endpoint**: `PUT /user/updateProfile/{authUserId}`  
**Gateway URL**: `http://localhost:7082/user/updateProfile/{authUserId}`  
**Description**: Update user profile information

**Path Parameters**:
- `authUserId`: User ID (Long)

**Request Body**:
```json
{
  "email": "john.updated@example.com",
  "firstName": "John",
  "lastName": "Doe Updated",
  "phoneNumber": "+1234567891",
  "userRole": "DRIVER",
  "city": "Boston"
}
```

**Response**: `200 OK` (No body)

**Example cURL**:
```bash
curl -X PUT http://localhost:7082/user/updateProfile/1 \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.updated@example.com",
    "firstName": "John",
    "lastName": "Doe Updated",
    "phoneNumber": "+1234567891",
    "userRole": "DRIVER",
    "city": "Boston"
  }'
```

---

### 2.3 Fetch User Profile
**Endpoint**: `GET /user/fetchProfile/{authUserId}`  
**Gateway URL**: `http://localhost:7082/user/fetchProfile/{authUserId}`  
**Description**: Get user profile by user ID

**Path Parameters**:
- `authUserId`: User ID (Long)

**Response**: `200 OK`
```json
{
  "userProfileId": 1,
  "authUserId": 1,
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "userRole": "USER",
  "phoneNumber": "+1234567890",
  "city": "New York"
}
```

**Example cURL**:
```bash
curl -X GET http://localhost:7082/user/fetchProfile/1
```

---

### 2.4 Fetch User Profiles by Role
**Endpoint**: `GET /user/fetchProfilesByRole/{role}`  
**Gateway URL**: `http://localhost:7082/user/fetchProfilesByRole/{role}`  
**Description**: Get all user profiles with a specific role

**Path Parameters**:
- `role`: User role (String) - one of: `ADMIN`, `USER`, `DRIVER`, `DISPATCHER`

**Response**: `200 OK`
```json
[
  {
    "userProfileId": 1,
    "authUserId": 1,
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "userRole": "DRIVER",
    "phoneNumber": "+1234567890",
    "city": "New York"
  },
  {
    "userProfileId": 2,
    "authUserId": 2,
    "email": "jane.doe@example.com",
    "firstName": "Jane",
    "lastName": "Doe",
    "userRole": "DRIVER",
    "phoneNumber": "+1234567891",
    "city": "Boston"
  }
]
```

**Example cURL**:
```bash
curl -X GET http://localhost:7082/user/fetchProfilesByRole/DRIVER
```

---

## 3. Order Service

### 3.1 Create Order
**Endpoint**: `POST /order/create`  
**Gateway URL**: `http://localhost:7082/order/create`  
**Description**: Create a new order

**Headers**:
- `X-User-Id`: User ID (Required)
- `Content-Type`: application/json

**Request Body**:
```json
{
  "from": "New York",
  "to": "Boston"
}
```

**Validation Rules**:
- `from`: Required, 2-50 characters
- `to`: Required, 2-50 characters

**Response**: `200 OK`
```json
{
  "orderId": 1,
  "from": "New York",
  "to": "Boston",
  "status": "CREATED",
  "createdAt": "2026-03-30T10:30:00",
  "deliveredAt": null,
  "vehicleId": null
}
```

**Example cURL**:
```bash
curl -X POST http://localhost:7082/order/create \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "from": "New York",
    "to": "Boston"
  }'
```

---

### 3.2 Assign Order to Vehicle
**Endpoint**: `POST /order/{orderId}/assign`  
**Gateway URL**: `http://localhost:7082/order/{orderId}/assign`  
**Description**: Automatically assign an available vehicle to an order

**Path Parameters**:
- `orderId`: Order ID (Long)

**Response**: `200 OK`
```json
{
  "orderId": 1,
  "from": "New York",
  "to": "Boston",
  "status": "ASSIGNED",
  "createdAt": "2026-03-30T10:30:00",
  "deliveredAt": null,
  "vehicleId": 5
}
```

**Example cURL**:
```bash
curl -X POST http://localhost:7082/order/1/assign
```

---

### 3.3 Update Order Status
**Endpoint**: `PUT /order/{orderId}/status`  
**Gateway URL**: `http://localhost:7082/order/{orderId}/status`  
**Description**: Update the status of an order

**Path Parameters**:
- `orderId`: Order ID (Long)

**Request Body**:
```json
{
  "status": "IN_TRANSIT"
}
```

**Status Options**:
- `CREATED`: Order has been created
- `ASSIGNED`: Order has been assigned to a vehicle
- `IN_TRANSIT`: Order is being transported
- `DELIVERED`: Order has been delivered

**Response**: `200 OK`
```json
{
  "orderId": 1,
  "from": "New York",
  "to": "Boston",
  "status": "IN_TRANSIT",
  "createdAt": "2026-03-30T10:30:00",
  "deliveredAt": null,
  "vehicleId": 5
}
```

**Example cURL**:
```bash
curl -X PUT http://localhost:7082/order/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_TRANSIT"}'
```

---

### 3.4 Fetch Order
**Endpoint**: `GET /order/{orderId}`  
**Gateway URL**: `http://localhost:7082/order/{orderId}`  
**Description**: Get order details by order ID

**Path Parameters**:
- `orderId`: Order ID (Long)

**Response**: `200 OK`
```json
{
  "orderId": 1,
  "from": "New York",
  "to": "Boston",
  "status": "IN_TRANSIT",
  "createdAt": "2026-03-30T10:30:00",
  "deliveredAt": null,
  "vehicleId": 5
}
```

**Example cURL**:
```bash
curl -X GET http://localhost:7082/order/1
```

---

### 3.5 Fetch All Orders
**Endpoint**: `GET /order/all`  
**Gateway URL**: `http://localhost:7082/order/all`  
**Description**: Get all orders in the system

**Response**: `200 OK`
```json
[
  {
    "orderId": 1,
    "from": "New York",
    "to": "Boston",
    "status": "IN_TRANSIT",
    "createdAt": "2026-03-30T10:30:00",
    "deliveredAt": null,
    "vehicleId": 5
  },
  {
    "orderId": 2,
    "from": "Chicago",
    "to": "Miami",
    "status": "CREATED",
    "createdAt": "2026-03-30T11:00:00",
    "deliveredAt": null,
    "vehicleId": null
  }
]
```

**Example cURL**:
```bash
curl -X GET http://localhost:7082/order/all
```

---

## 4. Vehicle Service

### 4.1 Create Vehicle
**Endpoint**: `POST /vehicle/create`  
**Gateway URL**: `http://localhost:7082/vehicle/create`  
**Description**: Register a new vehicle in the system

**Request Body**:
```json
{
  "vehicleName": "Express Truck 1",
  "vehicleType": "TRUCK",
  "vehicleCode": "TRK-001",
  "from": "New York",
  "to": "Boston",
  "through": ["Philadelphia", "New Haven"]
}
```

**Field Details**:
- `vehicleName`: Required
- `vehicleType`: Required, one of: `TEMPO`, `MINITRUCK`, `TRUCK`, `PICKUP`
- `vehicleCode`: Required, unique identifier
- `from`: Required, starting point (2-50 characters)
- `to`: Required, destination (2-50 characters)
- `through`: Required, list of intermediate cities (cannot be empty)

**Response**: `200 OK` (No body)

**Example cURL**:
```bash
curl -X POST http://localhost:7082/vehicle/create \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleName": "Express Truck 1",
    "vehicleType": "TRUCK",
    "vehicleCode": "TRK-001",
    "from": "New York",
    "to": "Boston",
    "through": ["Philadelphia", "New Haven"]
  }'
```

---

### 4.2 Update Vehicle
**Endpoint**: `PUT /vehicle/update`  
**Gateway URL**: `http://localhost:7082/vehicle/update`  
**Description**: Update vehicle information

**Request Body**:
```json
{
  "vehicleId": 1,
  "vehicleName": "Express Truck 1 Updated",
  "vehicleCode": "TRK-001",
  "vehicleType": "TRUCK",
  "from": "New York",
  "to": "Boston",
  "through": ["Philadelphia", "New Haven", "Providence"],
  "orderIds": [1, 2],
  "vehicleStatus": "OCCUPIED"
}
```

**Response**: `200 OK`
```json
{
  "vehicleId": 1,
  "driverId": null,
  "vehicleName": "Express Truck 1 Updated",
  "vehicleCode": "TRK-001",
  "vehicleType": "TRUCK",
  "from": "New York",
  "to": "Boston",
  "through": ["Philadelphia", "New Haven", "Providence"],
  "orderIds": [1, 2],
  "vehicleStatus": "OCCUPIED"
}
```

**Example cURL**:
```bash
curl -X PUT http://localhost:7082/vehicle/update \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleId": 1,
    "vehicleName": "Express Truck 1 Updated",
    "vehicleCode": "TRK-001",
    "vehicleType": "TRUCK",
    "from": "New York",
    "to": "Boston",
    "through": ["Philadelphia", "New Haven", "Providence"],
    "orderIds": [1, 2],
    "vehicleStatus": "OCCUPIED"
  }'
```

---

### 4.3 Find Idle Vehicles
**Endpoint**: `GET /vehicle/findIdle`  
**Gateway URL**: `http://localhost:7082/vehicle/findIdle`  
**Description**: Find available vehicles for assignment

**Query Parameters**:
- `vehicleType` (optional): Filter by vehicle type (`TEMPO`, `MINITRUCK`, `TRUCK`, `PICKUP`)
- `vehicleStatus` (optional): Filter by status (`IDLE`, `OCCUPIED`, `IN_TRANSIT`, `MAINTENANCE`, `DISCARDED`)
- `from` (required): Starting location

**Response**: `200 OK`
```json
[
  {
    "vehicleId": 1,
    "driverId": null,
    "vehicleName": "Express Truck 1",
    "vehicleCode": "TRK-001",
    "vehicleType": "TRUCK",
    "from": "New York",
    "to": "Boston",
    "through": ["Philadelphia", "New Haven"],
    "orderIds": [],
    "vehicleStatus": "IDLE"
  }
]
```

**Example cURL**:
```bash
curl -X GET "http://localhost:7082/vehicle/findIdle?from=New York&vehicleType=TRUCK"
```

---

### 4.4 Find Vehicles for Route
**Endpoint**: `GET /vehicle/route-match`  
**Gateway URL**: `http://localhost:7082/vehicle/route-match`  
**Description**: Find vehicles that match a specific route

**Query Parameters**:
- `from` (required): Starting location
- `to` (required): Destination
- `vehicleType` (optional): Filter by vehicle type

**Response**: `200 OK`
```json
[
  {
    "vehicleId": 1,
    "driverId": null,
    "vehicleName": "Express Truck 1",
    "vehicleCode": "TRK-001",
    "vehicleType": "TRUCK",
    "from": "New York",
    "to": "Boston",
    "through": ["Philadelphia", "New Haven"],
    "orderIds": [],
    "vehicleStatus": "IDLE"
  }
]
```

**Example cURL**:
```bash
curl -X GET "http://localhost:7082/vehicle/route-match?from=New York&to=Boston&vehicleType=TRUCK"
```

---

### 4.5 Assign Order to Vehicle
**Endpoint**: `POST /vehicle/{vehicleId}/orders/{orderId}`  
**Gateway URL**: `http://localhost:7082/vehicle/{vehicleId}/orders/{orderId}`  
**Description**: Manually assign an order to a specific vehicle

**Path Parameters**:
- `vehicleId`: Vehicle ID (Long)
- `orderId`: Order ID (Long)

**Response**: `200 OK`
```json
{
  "vehicleId": 1,
  "driverId": null,
  "vehicleName": "Express Truck 1",
  "vehicleCode": "TRK-001",
  "vehicleType": "TRUCK",
  "from": "New York",
  "to": "Boston",
  "through": ["Philadelphia", "New Haven"],
  "orderIds": [1],
  "vehicleStatus": "OCCUPIED"
}
```

**Example cURL**:
```bash
curl -X POST http://localhost:7082/vehicle/1/orders/1
```

---

### 4.6 Remove Order from Vehicle
**Endpoint**: `DELETE /vehicle/{vehicleId}/orders/{orderId}`  
**Gateway URL**: `http://localhost:7082/vehicle/{vehicleId}/orders/{orderId}`  
**Description**: Remove an order from a vehicle

**Path Parameters**:
- `vehicleId`: Vehicle ID (Long)
- `orderId`: Order ID (Long)

**Response**: `200 OK`
```json
{
  "vehicleId": 1,
  "driverId": null,
  "vehicleName": "Express Truck 1",
  "vehicleCode": "TRK-001",
  "vehicleType": "TRUCK",
  "from": "New York",
  "to": "Boston",
  "through": ["Philadelphia", "New Haven"],
  "orderIds": [],
  "vehicleStatus": "IDLE"
}
```

**Example cURL**:
```bash
curl -X DELETE http://localhost:7082/vehicle/1/orders/1
```

---

### 4.7 Update Vehicle Status
**Endpoint**: `PUT /vehicle/{vehicleId}/status`  
**Gateway URL**: `http://localhost:7082/vehicle/{vehicleId}/status`  
**Description**: Update the status of a vehicle

**Path Parameters**:
- `vehicleId`: Vehicle ID (Long)

**Query Parameters**:
- `vehicleStatus` (required): New status - one of: `IDLE`, `OCCUPIED`, `IN_TRANSIT`, `MAINTENANCE`, `DISCARDED`

**Response**: `200 OK`
```json
{
  "vehicleId": 1,
  "driverId": null,
  "vehicleName": "Express Truck 1",
  "vehicleCode": "TRK-001",
  "vehicleType": "TRUCK",
  "from": "New York",
  "to": "Boston",
  "through": ["Philadelphia", "New Haven"],
  "orderIds": [],
  "vehicleStatus": "MAINTENANCE"
}
```

**Example cURL**:
```bash
curl -X PUT "http://localhost:7082/vehicle/1/status?vehicleStatus=MAINTENANCE"
```

---

### 4.8 Assign Driver to Vehicle
**Endpoint**: `POST /vehicle/assignDriver/{vehicleId}/{driverId}`  
**Gateway URL**: `http://localhost:7082/vehicle/assignDriver/{vehicleId}/{driverId}`  
**Description**: Assign a driver to a vehicle

**Path Parameters**:
- `vehicleId`: Vehicle ID (Long)
- `driverId`: Driver's user profile ID (Long)

**Response**: `200 OK`
```json
{
  "vehicleId": 1,
  "vehicleName": "Express Truck 1",
  "vehicleCode": "TRK-001",
  "vehicleType": "TRUCK",
  "from": "New York",
  "to": "Boston",
  "through": ["Philadelphia", "New Haven"],
  "orderIds": [],
  "vehicleStatus": "IDLE",
  "driver": {
    "userProfileId": 2,
    "firstName": "John",
    "lastName": "Driver",
    "email": "john.driver@example.com",
    "phoneNumber": "+1234567890",
    "city": "New York",
    "userRole": "DRIVER"
  }
}
```

**Example cURL**:
```bash
curl -X POST http://localhost:7082/vehicle/assignDriver/1/2
```

---

### 4.9 Unassign Driver from Vehicle
**Endpoint**: `POST /vehicle/unassignDriver/{vehicleId}`  
**Gateway URL**: `http://localhost:7082/vehicle/unassignDriver/{vehicleId}`  
**Description**: Remove driver assignment from a vehicle

**Path Parameters**:
- `vehicleId`: Vehicle ID (Long)

**Response**: `200 OK`
```json
{
  "vehicleId": 1,
  "vehicleName": "Express Truck 1",
  "vehicleCode": "TRK-001",
  "vehicleType": "TRUCK",
  "from": "New York",
  "to": "Boston",
  "through": ["Philadelphia", "New Haven"],
  "orderIds": [],
  "vehicleStatus": "IDLE",
  "driver": null
}
```

**Example cURL**:
```bash
curl -X POST http://localhost:7082/vehicle/unassignDriver/1
```

---

### 4.10 Get Vehicle with Driver Information
**Endpoint**: `GET /vehicle/{vehicleId}`  
**Gateway URL**: `http://localhost:7082/vehicle/{vehicleId}`  
**Description**: Get detailed vehicle information including driver details

**Path Parameters**:
- `vehicleId`: Vehicle ID (Long)

**Response**: `200 OK`
```json
{
  "vehicleId": 1,
  "vehicleName": "Express Truck 1",
  "vehicleCode": "TRK-001",
  "vehicleType": "TRUCK",
  "from": "New York",
  "to": "Boston",
  "through": ["Philadelphia", "New Haven"],
  "orderIds": [1],
  "vehicleStatus": "OCCUPIED",
  "driver": {
    "userProfileId": 2,
    "firstName": "John",
    "lastName": "Driver",
    "email": "john.driver@example.com",
    "phoneNumber": "+1234567890",
    "city": "New York",
    "userRole": "DRIVER"
  }
}
```

**Example cURL**:
```bash
curl -X GET http://localhost:7082/vehicle/1
```

---

### 4.11 Get All Vehicles with Drivers
**Endpoint**: `GET /vehicle/all/withDrivers`  
**Gateway URL**: `http://localhost:7082/vehicle/all/withDrivers`  
**Description**: Get all vehicles with their driver information

**Response**: `200 OK`
```json
[
  {
    "vehicleId": 1,
    "vehicleName": "Express Truck 1",
    "vehicleCode": "TRK-001",
    "vehicleType": "TRUCK",
    "from": "New York",
    "to": "Boston",
    "through": ["Philadelphia", "New Haven"],
    "orderIds": [],
    "vehicleStatus": "IDLE",
    "driver": {
      "userProfileId": 2,
      "firstName": "John",
      "lastName": "Driver",
      "email": "john.driver@example.com",
      "phoneNumber": "+1234567890",
      "city": "New York",
      "userRole": "DRIVER"
    }
  }
]
```

**Example cURL**:
```bash
curl -X GET http://localhost:7082/vehicle/all/withDrivers
```

---

### 4.12 Get Occupied Vehicles with Drivers
**Endpoint**: `GET /vehicle/occupied/withDrivers`  
**Gateway URL**: `http://localhost:7082/vehicle/occupied/withDrivers`  
**Description**: Get all occupied vehicles with their driver information

**Response**: `200 OK`
```json
[
  {
    "vehicleId": 1,
    "vehicleName": "Express Truck 1",
    "vehicleCode": "TRK-001",
    "vehicleType": "TRUCK",
    "from": "New York",
    "to": "Boston",
    "through": ["Philadelphia", "New Haven"],
    "orderIds": [1, 2],
    "vehicleStatus": "OCCUPIED",
    "driver": {
      "userProfileId": 2,
      "firstName": "John",
      "lastName": "Driver",
      "email": "john.driver@example.com",
      "phoneNumber": "+1234567890",
      "city": "New York",
      "userRole": "DRIVER"
    }
  }
]
```

**Example cURL**:
```bash
curl -X GET http://localhost:7082/vehicle/occupied/withDrivers
```

---

### 4.13 Get Driver's Current Vehicle
**Endpoint**: `GET /vehicle/driver/{driverId}/currentVehicle`  
**Gateway URL**: `http://localhost:7082/vehicle/driver/{driverId}/currentVehicle`  
**Description**: Get the current vehicle assigned to a specific driver

**Path Parameters**:
- `driverId`: Driver's user profile ID (Long)

**Response**: `200 OK`
```json
{
  "vehicleId": 1,
  "vehicleName": "Express Truck 1",
  "vehicleCode": "TRK-001",
  "vehicleType": "TRUCK",
  "from": "New York",
  "to": "Boston",
  "through": ["Philadelphia", "New Haven"],
  "orderIds": [1],
  "vehicleStatus": "OCCUPIED",
  "driver": {
    "userProfileId": 2,
    "firstName": "John",
    "lastName": "Driver",
    "email": "john.driver@example.com",
    "phoneNumber": "+1234567890",
    "city": "New York",
    "userRole": "DRIVER"
  }
}
```

**Example cURL**:
```bash
curl -X GET http://localhost:7082/vehicle/driver/2/currentVehicle
```

---

## 5. Enumerations Reference

### User Roles
- `ADMIN`: Administrator with full system access
- `USER`: Regular user who can create orders
- `DRIVER`: Driver who operates vehicles
- `DISPATCHER`: Person who manages order assignments

### Driver Status
- `ACTIVE`: Driver is available for assignments
- `OCCUPIED`: Driver is currently on a trip
- `INACTIVE`: Driver is not available

### Vehicle Types
- `TEMPO`: Small commercial vehicle
- `MINITRUCK`: Medium-sized truck
- `TRUCK`: Large truck
- `PICKUP`: Pickup truck

### Vehicle Status
- `IDLE`: Vehicle is available for assignment
- `OCCUPIED`: Vehicle has orders assigned but not yet in transit
- `IN_TRANSIT`: Vehicle is currently on the road
- `MAINTENANCE`: Vehicle is under maintenance
- `DISCARDED`: Vehicle is no longer in service

### Order Status
- `CREATED`: Order has been created
- `ASSIGNED`: Order has been assigned to a vehicle
- `IN_TRANSIT`: Order is being transported
- `DELIVERED`: Order has been successfully delivered

---

## 6. Testing Workflow Examples

### Complete Order Fulfillment Flow

1. **Register a User**:
```bash
curl -X POST http://localhost:7082/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test@1234"}'
```

2. **Login**:
```bash
curl -X POST http://localhost:7082/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test@1234"}'
```
Save the `userId` and `token` from response.

3. **Create User Profile**:
```bash
curl -X POST http://localhost:7082/user/createProfile \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "userRole": "USER",
    "city": "New York"
  }'
```

4. **Create a Driver Profile**:
```bash
# First register and login as driver
curl -X POST http://localhost:7082/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"driver1","password":"Driver@123"}'

curl -X POST http://localhost:7082/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"driver1","password":"Driver@123"}'

# Create driver profile (use userId from driver login)
curl -X POST http://localhost:7082/user/createProfile \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{
    "email": "driver@example.com",
    "firstName": "John",
    "lastName": "Driver",
    "userRole": "DRIVER",
    "city": "New York",
    "phoneNumber": "+1234567890"
  }'
```

5. **Create a Vehicle**:
```bash
curl -X POST http://localhost:7082/vehicle/create \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleName": "Express Truck 1",
    "vehicleType": "TRUCK",
    "vehicleCode": "TRK-001",
    "from": "New York",
    "to": "Boston",
    "through": ["Philadelphia", "New Haven"]
  }'
```

6. **Assign Driver to Vehicle** (assuming vehicleId=1, driverId=2):
```bash
curl -X POST http://localhost:7082/vehicle/assignDriver/1/2
```

7. **Create an Order**:
```bash
curl -X POST http://localhost:7082/order/create \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"from": "New York", "to": "Boston"}'
```

8. **Assign Vehicle to Order** (assuming orderId=1):
```bash
curl -X POST http://localhost:7082/order/1/assign
```

9. **Update Order Status**:
```bash
# Mark as in transit
curl -X PUT http://localhost:7082/order/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_TRANSIT"}'

# Mark as delivered
curl -X PUT http://localhost:7082/order/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"DELIVERED"}'
```

10. **Check Order Details**:
```bash
curl -X GET http://localhost:7082/order/1
```

---

## 7. Common HTTP Status Codes

- `200 OK`: Request succeeded
- `201 Created`: Resource created successfully
- `400 Bad Request`: Invalid request data or validation error
- `401 Unauthorized`: Authentication required or failed
- `404 Not Found`: Requested resource not found
- `500 Internal Server Error`: Server-side error

---

## 8. Notes

- All timestamps are in ISO-8601 format (e.g., `2026-03-30T10:30:00`)
- JWT tokens should be included in the `Authorization` header as `Bearer <token>` for protected endpoints (if JWT authentication is fully implemented)
- The API Gateway performs routing and may add additional headers
- Rate limiting is enabled at the gateway level (100 requests per minute)
- All services are registered with Eureka Service Discovery on `http://localhost:8761`

---

## 9. Services Not Yet Implemented

Based on the codebase analysis, the following services are configured but do not have controllers yet:

- **Tracking Service** (Port 7085): Reserved for future order tracking functionality
- **Analytics Service** (Port 7088): Reserved for future analytics and reporting

These services currently only have their main application classes and no REST endpoints.

