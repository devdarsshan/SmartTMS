# Vehicle Service - Quick API Reference Guide

## Base URL
```
http://localhost:7087/vehicle
```

---

## 1️⃣ CREATE VEHICLE

**Endpoint:** `POST /vehicle/create`  
**Content-Type:** `application/json`

### Request Body
```json
{
  "vehicleName": "Delivery Van 01",
  "vehicleCode": "VH-001",
  "vehicleType": "TEMPO",
  "registeredCity": "Mumbai"
}
```

### Response (200 OK)
```
OK
```

### Possible Errors
- `409 Conflict` - Vehicle code already exists
- `400 Bad Request` - Invalid input

---

## 2️⃣ ASSIGN DRIVER TO VEHICLE ⭐

**Endpoint:** `POST /vehicle/assignDriver/{vehicleId}/{driverId}`

### Path Parameters
- `vehicleId` - ID of the vehicle
- `driverId` - User profile ID of the driver

### Response (200 OK)
```json
{
  "vehicleId": 1,
  "vehicleName": "Delivery Van 01",
  "vehicleCode": "VH-001",
  "vehicleType": "TEMPO",
  "registeredCity": "Mumbai",
  "vehicleStatus": "OCCUPIED",
  "driver": {
    "userProfileId": 5,
    "firstName": "Raj",
    "lastName": "Kumar",
    "email": "raj@example.com",
    "phoneNumber": "9876543210",
    "city": "Mumbai",
    "userRole": "DRIVER"
  }
}
```

### Possible Errors
- `404 Not Found` - Vehicle not found
- `404 Not Found` - Driver not found
- `400 Bad Request` - Vehicle already occupied

---

## 3️⃣ UNASSIGN DRIVER FROM VEHICLE ⭐

**Endpoint:** `POST /vehicle/unassignDriver/{vehicleId}`

### Path Parameters
- `vehicleId` - ID of the vehicle

### Response (200 OK)
```json
{
  "vehicleId": 1,
  "vehicleName": "Delivery Van 01",
  "vehicleCode": "VH-001",
  "vehicleType": "TEMPO",
  "registeredCity": "Mumbai",
  "vehicleStatus": "IDLE",
  "driver": {
    "userProfileId": 5,
    "firstName": "Raj",
    "lastName": "Kumar",
    "email": "raj@example.com",
    "phoneNumber": "9876543210",
    "city": "Mumbai",
    "userRole": "DRIVER"
  }
}
```

### Possible Errors
- `404 Not Found` - Vehicle not found
- `400 Bad Request` - No driver assigned to vehicle

---

## 4️⃣ GET VEHICLE WITH DRIVER ⭐

**Endpoint:** `GET /vehicle/{vehicleId}`

### Path Parameters
- `vehicleId` - ID of the vehicle

### Response (200 OK)
```json
{
  "vehicleId": 1,
  "vehicleName": "Delivery Van 01",
  "vehicleCode": "VH-001",
  "vehicleType": "TEMPO",
  "registeredCity": "Mumbai",
  "vehicleStatus": "OCCUPIED",
  "driver": {
    "userProfileId": 5,
    "firstName": "Raj",
    "lastName": "Kumar",
    "email": "raj@example.com",
    "phoneNumber": "9876543210",
    "city": "Mumbai",
    "userRole": "DRIVER"
  }
}
```

### Possible Errors
- `404 Not Found` - Vehicle not found

---

## 5️⃣ GET DRIVER'S CURRENT VEHICLE ⭐

**Endpoint:** `GET /vehicle/driver/{driverId}/currentVehicle`

### Path Parameters
- `driverId` - User profile ID of the driver

### Response (200 OK)
```json
{
  "vehicleId": 1,
  "vehicleName": "Delivery Van 01",
  "vehicleCode": "VH-001",
  "vehicleType": "TEMPO",
  "registeredCity": "Mumbai",
  "vehicleStatus": "OCCUPIED",
  "driver": {
    "userProfileId": 5,
    "firstName": "Raj",
    "lastName": "Kumar",
    "email": "raj@example.com",
    "phoneNumber": "9876543210",
    "city": "Mumbai",
    "userRole": "DRIVER"
  }
}
```

### Possible Errors
- `404 Not Found` - Driver not found
- `400 Bad Request` - Driver not assigned to any vehicle

---

## 6️⃣ GET ALL OCCUPIED VEHICLES ⭐

**Endpoint:** `GET /vehicle/occupied/withDrivers`

### Response (200 OK)
```json
[
  {
    "vehicleId": 1,
    "vehicleName": "Delivery Van 01",
    "vehicleCode": "VH-001",
    "vehicleType": "TEMPO",
    "registeredCity": "Mumbai",
    "vehicleStatus": "OCCUPIED",
    "driver": {
      "userProfileId": 5,
      "firstName": "Raj",
      "lastName": "Kumar",
      "email": "raj@example.com",
      "phoneNumber": "9876543210",
      "city": "Mumbai",
      "userRole": "DRIVER"
    }
  },
  {
    "vehicleId": 2,
    "vehicleName": "Truck 02",
    "vehicleCode": "VH-002",
    "vehicleType": "TRUCK",
    "registeredCity": "Delhi",
    "vehicleStatus": "OCCUPIED",
    "driver": {
      "userProfileId": 6,
      "firstName": "Priya",
      "lastName": "Singh",
      "email": "priya@example.com",
      "phoneNumber": "9876543211",
      "city": "Delhi",
      "userRole": "DRIVER"
    }
  }
]
```

### Possible Errors
- None (returns empty list if no occupied vehicles)

---

## 7️⃣ GET ALL VEHICLES WITH DRIVERS

**Endpoint:** `GET /vehicle/all/withDrivers`

### Response (200 OK)
```json
[
  {
    "vehicleId": 1,
    "vehicleName": "Delivery Van 01",
    "vehicleCode": "VH-001",
    "vehicleType": "TEMPO",
    "registeredCity": "Mumbai",
    "vehicleStatus": "OCCUPIED",
    "driver": { /* driver info */ }
  },
  {
    "vehicleId": 3,
    "vehicleName": "Pickup 03",
    "vehicleCode": "VH-003",
    "vehicleType": "PICKUP",
    "registeredCity": "Bangalore",
    "vehicleStatus": "IDLE",
    "driver": null
  }
]
```

---

## 8️⃣ UPDATE VEHICLE

**Endpoint:** `PUT /vehicle/update`  
**Content-Type:** `application/json`

### Request Body
```json
{
  "vehicleId": 1,
  "vehicleName": "Updated Van Name",
  "vehicleCode": "VH-001",
  "vehicleType": "TEMPO",
  "registeredCity": "Mumbai",
  "vehicleStatus": "IDLE"
}
```

### Response (200 OK)
```json
{
  "vehicleId": 1,
  "vehicleName": "Updated Van Name",
  "vehicleCode": "VH-001",
  "vehicleType": "TEMPO",
  "registeredCity": "Mumbai",
  "vehicleStatus": "IDLE",
  "driverId": null
}
```

---

## 9️⃣ FIND IDLE VEHICLES

**Endpoint:** `GET /vehicle/findIdle`

### Query Parameters
- `vehicleType` - TEMPO, MINITRUCK, TRUCK, or PICKUP
- `vehicleStatus` - IDLE, OCCUPIED, MAINTENANCE, or DISCARDED
- `registeredCity` - City name

### Example
```
GET /vehicle/findIdle?vehicleType=TEMPO&vehicleStatus=IDLE&registeredCity=Mumbai
```

### Response (200 OK)
```json
[
  {
    "vehicleId": 3,
    "vehicleName": "Pickup 03",
    "vehicleCode": "VH-003",
    "vehicleType": "PICKUP",
    "registeredCity": "Mumbai",
    "vehicleStatus": "IDLE",
    "driverId": null
  }
]
```

---

## VEHICLE TYPES
- `TEMPO`
- `MINITRUCK`
- `TRUCK`
- `PICKUP`

## VEHICLE STATUSES
- `IDLE` - Vehicle is available
- `OCCUPIED` - Vehicle is assigned to a driver
- `MAINTENANCE` - Vehicle is under maintenance
- `DISCARDED` - Vehicle is no longer in use

---

## HTTP STATUS CODES

| Status | Meaning |
|--------|---------|
| 200 | Success |
| 400 | Bad Request (invalid input or operation) |
| 404 | Not Found (vehicle/driver doesn't exist) |
| 409 | Conflict (vehicle code already exists) |
| 500 | Internal Server Error |

---

## cURL EXAMPLES

### Assign Driver
```bash
curl -X POST "http://localhost:7087/vehicle/assignDriver/1/5" \
  -H "Content-Type: application/json"
```

### Get Driver's Vehicle
```bash
curl -X GET "http://localhost:7087/vehicle/driver/5/currentVehicle" \
  -H "Content-Type: application/json"
```

### Get Occupied Vehicles
```bash
curl -X GET "http://localhost:7087/vehicle/occupied/withDrivers" \
  -H "Content-Type: application/json"
```

### Create Vehicle
```bash
curl -X POST "http://localhost:7087/vehicle/create" \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleName": "Van 01",
    "vehicleCode": "VH-001",
    "vehicleType": "TEMPO",
    "registeredCity": "Mumbai"
  }'
```

---

## ⭐ NEW FEATURES (TODO Completion)
These endpoints implement the 5 TODO items:
1. ✅ Assign Driver to Vehicle
2. ✅ Unassign Driver from Vehicle
3. ✅ Get Vehicle's Current Driver
4. ✅ Get Driver's Current Vehicle
5. ✅ Get All Occupied Vehicles with Drivers

---

## NOTES
- All operations are logged
- Feign Client communicates with user-service on port 8081
- Driver information is fetched from user-service
- Vehicle status automatically updates on assignment/unassignment

