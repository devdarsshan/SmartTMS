# Kafka Setup Guide For SmartTMS

This guide shows how to install, configure, start, and verify Kafka on a Windows laptop for this project.

It is written for the current SmartTMS repo, where the services already default to:

- `KAFKA_BOOTSTRAP_SERVERS=localhost:9092`
- `ORDER_EVENTS_TOPIC=order-events`
- `VEHICLE_EVENTS_TOPIC=vehicle-events`
- `VEHICLE_LOCATION_EVENTS_TOPIC=vehicle-location-events`

Recommended setup for this project:

- Apache Kafka in **KRaft** mode
- single-node local broker
- no ZooKeeper
- broker reachable at `localhost:9092`

Official references:

- Kafka downloads: https://kafka.apache.org/downloads.html
- Kafka quick start: https://kafka.apache.org/40/getting-started/quickstart/
- KRaft docs: https://kafka.apache.org/40/operations/kraft/

## 1. Prerequisites

Install these first:

- Java 17 or newer
- PowerShell
- Enough free disk space for Kafka logs

This repo uses Java 21, so if your services already run, your Java is probably fine.

Check Java:

```powershell
java -version
```

If Java is not installed or not on PATH, fix that first.

## 2. Download Kafka

1. Open the official download page:
   `https://kafka.apache.org/downloads.html`
2. Download a recent binary release.
   Example naming:
   `kafka_2.13-4.1.0.tgz`
3. Extract it somewhere simple, for example:
   `C:\tools\kafka`

After extraction, your Kafka folder should look something like:

```text
C:\tools\kafka
  ├─ bin
  ├─ config
  ├─ libs
  └─ licenses
```

## 3. Create Local Kafka Data Folders

Create a separate place for local broker data:

```powershell
New-Item -ItemType Directory -Force C:\kafka\data | Out-Null
```

Optional log folder:

```powershell
New-Item -ItemType Directory -Force C:\kafka\logs | Out-Null
```

## 4. Configure Kafka For Local SmartTMS Use

Open:

`C:\tools\kafka\config\server.properties`

For a simple local setup, make sure these values are present.

```properties
listeners=PLAINTEXT://localhost:9092,CONTROLLER://localhost:9093
advertised.listeners=PLAINTEXT://localhost:9092
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
inter.broker.listener.name=PLAINTEXT
controller.listener.names=CONTROLLER
log.dirs=C:/kafka/data
num.partitions=3
auto.create.topics.enable=true
```

Notes:

- `advertised.listeners=PLAINTEXT://localhost:9092` is important. Your Spring services connect to Kafka through `localhost:9092`.
- `log.dirs` should point to a real writable folder.
- `num.partitions=3` is a nice default for this personal project. It is not mandatory.
- `auto.create.topics.enable=true` makes local development easier, but we will still create the topics manually below so everything is explicit.

Do not overcomplicate the config. For this project, a single local broker is enough.

## 5. Initialize Kafka Storage For KRaft

Kafka in KRaft mode needs a cluster id and storage formatting before first startup.

Open PowerShell in your Kafka folder:

```powershell
Set-Location C:\tools\kafka
```

Generate a cluster id:

```powershell
.\bin\windows\kafka-storage.bat random-uuid
```

Copy the generated value, then format storage:

```powershell
.\bin\windows\kafka-storage.bat format --standalone -t YOUR_CLUSTER_ID_HERE -c .\config\server.properties
```

Important:

- do this once for a fresh data directory
- if you later delete `C:\kafka\data`, you must format again
- do not repeatedly re-format the same storage unless you intentionally want a fresh local cluster

## 6. Start Kafka

From the Kafka root:

```powershell
Set-Location C:\tools\kafka
.\bin\windows\kafka-server-start.bat .\config\server.properties
```

Leave this terminal window open. That terminal is now your running Kafka broker.

If startup is successful, Kafka will keep running and listen on:

- broker: `localhost:9092`
- controller: `localhost:9093`

## 7. Create The Topics Used By SmartTMS

Open a second PowerShell window.

Go to Kafka:

```powershell
Set-Location C:\tools\kafka
```

Create the required topics:

```powershell
.\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --create --topic order-events --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --create --topic vehicle-events --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --create --topic vehicle-location-events --partitions 3 --replication-factor 1
```

List topics to confirm:

```powershell
.\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --list
```

You should see:

- `order-events`
- `vehicle-events`
- `vehicle-location-events`

## 8. Set Environment Variables For SmartTMS

If you keep Kafka on the default address, you do not strictly need to set anything because the services already default to `localhost:9092`.

Still, it is a good idea to set it explicitly in the same terminal session where you start the apps:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
```

Optional explicit topic overrides:

```powershell
$env:ORDER_EVENTS_TOPIC="order-events"
$env:VEHICLE_EVENTS_TOPIC="vehicle-events"
$env:VEHICLE_LOCATION_EVENTS_TOPIC="vehicle-location-events"
```

## 9. Start SmartTMS Services

Once Kafka is running, start your services.

Suggested order:

1. Eureka
2. Auth service
3. User service
4. Vehicle service
5. Order service
6. Tracking service
7. Analytics service
8. API gateway

If you use separate terminals, set the Kafka env var in each terminal before starting the service:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
```

## 10. Verify Kafka Is Working With This Project

### A. Watch messages from a topic

Open another PowerShell window:

```powershell
Set-Location C:\tools\kafka
.\bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic vehicle-location-events --from-beginning
```

When tracking updates are published, you should see JSON events here.

You can do the same for:

```powershell
.\bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic order-events --from-beginning
.\bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic vehicle-events --from-beginning
```

### B. Generate tracking traffic

Use the simulator script already added to this repo:

[live_location_provider.py](/f:/Gen_Projects/SmartTMS/scripts/live_location_provider.py)

Example:

```powershell
python .\scripts\live_location_provider.py `
  --username driver.arjun `
  --password Driver@123 `
  --vehicle-id 1
```

That script keeps sending location updates until you stop it with `Ctrl+C`.

### C. Check tracking API

Once updates are flowing:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:7082/track/vehicle/1" -Headers @{
  Authorization = "Bearer YOUR_JWT"
}
```

### D. Check analytics API

After events are consumed:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:7082/analytics/overview" -Headers @{
  Authorization = "Bearer YOUR_ADMIN_OR_DISPATCHER_JWT"
}
```

## 11. Stop Kafka

In the Kafka server terminal, press:

```text
Ctrl + C
```

That stops the local broker.

## 12. Start Kafka Again Later

After the first-time formatting is done, normal restart is simple:

```powershell
Set-Location C:\tools\kafka
.\bin\windows\kafka-server-start.bat .\config\server.properties
```

You do not need to format storage again unless you deleted the Kafka data directory.

## 13. Common Problems And Fixes

### Problem: `java` is not recognized

Fix:

- install Java 17+
- add Java to PATH
- reopen PowerShell

### Problem: Kafka starts but services cannot connect

Check:

- Kafka is really listening on `localhost:9092`
- `advertised.listeners=PLAINTEXT://localhost:9092`
- service terminals have `KAFKA_BOOTSTRAP_SERVERS=localhost:9092` if you set overrides

### Problem: Port 9092 is already in use

Check what is using it:

```powershell
netstat -ano | findstr :9092
```

Either stop the conflicting process or change Kafka to a different port and also change:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:YOUR_PORT"
```

### Problem: Topic already exists

That is harmless if the topic name matches what the project expects.

### Problem: You reformatted Kafka and lost old topic data

That is expected. Formatting creates a fresh local cluster state.

### Problem: Tracking events are not appearing

Check:

- the driver is correctly assigned to the vehicle
- the tracking service is running
- the simulator is logging in successfully
- the topic `vehicle-location-events` exists

### Problem: Analytics is not updating

Check:

- `analytics-service` is running
- `order-events`, `vehicle-events`, and `vehicle-location-events` exist
- events are visible in console consumers

## 14. Quick Start Summary

If you want the shortest working path:

1. Install Java 17+
2. Download and extract Kafka
3. Edit `config\server.properties` so Kafka advertises `localhost:9092`
4. Run:

```powershell
Set-Location C:\tools\kafka
.\bin\windows\kafka-storage.bat random-uuid
.\bin\windows\kafka-storage.bat format --standalone -t YOUR_CLUSTER_ID -c .\config\server.properties
.\bin\windows\kafka-server-start.bat .\config\server.properties
```

5. In a second terminal, create topics:

```powershell
.\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --create --topic order-events --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --create --topic vehicle-events --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --create --topic vehicle-location-events --partitions 3 --replication-factor 1
```

6. Start SmartTMS services
7. Run the simulator script
8. Watch topic traffic with `kafka-console-consumer.bat`

## 15. Project-Specific Kafka Checklist

Before saying Kafka is ready for SmartTMS, confirm all of these are true:

- Kafka broker is running on `localhost:9092`
- topics exist:
  - `order-events`
  - `vehicle-events`
  - `vehicle-location-events`
- service terminals can see `KAFKA_BOOTSTRAP_SERVERS=localhost:9092`
- `order-service` can publish
- `vehicle-service` can publish
- `tracking-service` can publish and consume
- `analytics-service` can consume

Once that checklist passes, your Kafka setup is good for this repo.
