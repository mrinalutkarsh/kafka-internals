# 🚀 Kafka Internals – How to Run (First Time vs Subsequent Runs)

This project runs **Kafka, Kafka UI, and the Producer inside Docker**  
to avoid advertised listener and networking issues.

All services communicate using:

```

kafka:9092

````

---

## 🟢 First-Time Setup (Fresh Machine / Fresh Clone)

> Use this **ONLY once** or when you want a completely clean state.

### 1️⃣ Build the application JAR (fat JAR)

```bash
mvn clean package
````

✔ Produces a runnable JAR with all Kafka dependencies.

Verify:

```bash
ls target/
```

You should see:

```
kafka-internals-1.0.0-SNAPSHOT.jar
```

---

### 2️⃣ Start Kafka, Kafka UI, and Producer (clean state)

```bash
docker-compose down -v
docker-compose up -d --build
```

What this does:

* `down -v` → removes old Kafka data, topics, offsets
* `--build` → rebuilds producer Docker image
* Starts:

  * Zookeeper
  * Kafka broker
  * Kafka UI
  * Producer

⏳ Wait ~30 seconds for Kafka to fully start.

---

### 3️⃣ Create application topic (ONE TIME per clean start)

```bash
docker exec -it kafka bash
```

```bash
kafka-topics \
  --bootstrap-server kafka:9092 \
  --create \
  --topic order-events \
  --partitions 6 \
  --replication-factor 1
```

Verify:

```bash
kafka-topics --bootstrap-server kafka:9092 --list
```

Expected:

```
order-events
```

Exit container:

```bash
exit
```

---

### 4️⃣ Verify everything is working

#### Producer logs

```bash
docker logs kafka-producer
```

Expected:

```
Produced event | orderId=... | partition=... | offset=...
```

#### Kafka UI

Open:

```
http://localhost:8080
```

* Cluster: **Online**
* Brokers: **1**
* Topics: `order-events`
* Messages tab loads correctly

✅ **First-time setup complete**

---

## 🔁 Subsequent Runs (Normal Development)

> Use this for **daily development**.

### 1️⃣ Start everything (no data loss)

```bash
docker-compose up -d
```

✔ Kafka keeps topics
✔ Offsets preserved
✔ Faster startup

---

### 2️⃣ View logs if needed

```bash
docker logs kafka-producer
docker logs kafka
docker logs kafka-ui
```

---

### 3️⃣ Stop everything (without deleting data)

```bash
docker-compose down
```

---

## 🧪 Useful Verification Commands

### List topics

```bash
docker exec -it kafka kafka-topics \
  --bootstrap-server kafka:9092 \
  --list
```

---

### Describe topic

```bash
docker exec -it kafka kafka-topics \
  --bootstrap-server kafka:9092 \
  --describe \
  --topic order-events
```

---

### Consume messages manually

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic order-events \
  --from-beginning \
  --property print.partition=true \
  --property print.offset=true
```

---

## ⚠️ Important Rules (Read This Once)

### ❌ Do NOT run producer from host JVM

Producer must run **inside Docker**, otherwise:

* Advertised listeners break
* Metadata becomes unreachable
* Kafka UI & producer time out

---

### ❌ Do NOT mix `localhost` and `kafka` addresses

Inside Docker:

```
kafka:9092 ✅
localhost ❌
```

---

### ❌ Do NOT use `docker-compose down -v` casually

That command:

* Deletes ALL topics
* Deletes ALL offsets
* Requires re-creating topics

Use it **only** when you want a clean slate.

---

## 🧠 Mental Model (Remember This)

```
Docker Network
  ├── Kafka (kafka:9092)
  ├── Producer
  └── Kafka UI
```

One broker identity.
One advertised listener.
No ambiguity.

---

## ✅ Status Checklist

Before coding consumers, ensure:

* [x] Producer sends messages
* [x] Kafka UI shows messages
* [x] No timeouts
* [x] No advertised listener issues

---
