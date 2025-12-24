# [For instructions on running the application, see here.](/RUNBOOK.md)
# kafka-internals

Hands-on Java experiments exploring Apache Kafka internals — partitions, offsets, consumer groups, ordering guarantees, replication, leader election, and schema evolution using Avro, with observability via Kafka UI.

---

## 📌 Goal of This Repository

This repository is **not** a business application.

Its goal is to **deeply understand Kafka internals by building one coherent, end-to-end system** where Kafka concepts are *required*, not artificially demonstrated.

By the end, you should be able to confidently explain:

- Why Kafka works the way it does?
- How ordering, offsets, and consumer groups really behave
- What happens during failures and rebalances
- How schema evolution works in production systems

---

## 🧠 End-to-End Use Case

### **Distributed Order & Inventory Event Processing System**

A simplified event-driven system inspired by e-commerce workflows, designed specifically to expose Kafka internals.

### Business Flow

1. User places an order
2. Order goes through multiple stages:
   - `ORDER_CREATED`
   - `PAYMENT_COMPLETED`
   - `INVENTORY_RESERVED`
   - `SHIPPED`
3. Multiple independent services consume the same event stream
4. Ordering must be guaranteed **per order**
5. Failures must not cause double processing
6. Consumers must scale horizontally
7. Only one instance should act as a leader for certain operations
8. Event schemas must evolve safely over time

---

## 🔗 Why Kafka Is Required Here

| Kafka Concept | Why It Is Needed |
|--------------|------------------|
| Topics | Event stream per domain |
| Partitions | Scalability + ordering |
| Message ordering | Guaranteed per `orderId` |
| Consumer groups | Horizontal scaling |
| Multiple consumers | Independent services |
| Offset management | Exactly-once–style safety |
| Avro schemas | Safe schema evolution |
| Leader election | Single active coordinator |
| Kafka UI | Observability & debugging |

---

## 🧱 Architecture Overview

```

OrderService (Producer)
|
v
order-events topic (partitioned by orderId)
|
|----------------------------|
v                            v
PaymentConsumer             InventoryConsumer
(consumer-group-payment)    (consumer-group-inventory)
|
v
ShippingConsumer

````

Each **consumer group** receives the full event stream.  
Each **partition** guarantees ordering per `orderId`.

---

## 🧵 Topic Design

### `order-events`
- Partitions: 6
- Replication factor: 3
- Key: `orderId`
- Value: `OrderEvent` (Avro)

Optional supporting topics:
- `inventory-events`
- `dead-letter-events`
- `leader-election` (compacted)

---

## 📦 Event Schema (Avro)

```json
{
  "type": "record",
  "name": "OrderEvent",
  "namespace": "com.example.kafka.avro",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "userId", "type": "string"},
    {"name": "status", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "timestamp", "type": "long"}
  ]
}
````

### Schema Evolution (Later)

* Add optional fields (e.g., `currency`)
* Maintain backward compatibility
* Validate via Schema Registry

---

## 📁 Project Structure

```
src
 └── main
     ├── avro/              # Avro schemas (.avsc)
     ├── proto/             # Protobuf schemas (optional comparison)
     ├── java/com/example/kafka
     │   ├── basics/        # Simple producer & consumer
     │   ├── topics/        # Topic creation & configs
     │   ├── consumer/      # Consumer groups & rebalancing
     │   ├── offset/        # Manual offset management
     │   ├── leader/        # Leader election using Kafka
     │   ├── serialization/ # Avro / Protobuf producers & consumers
     │   └── ui/            # Kafka UI & observability helpers
     └── resources/
```

---

## 📂 Module Breakdown

### `basics/`

* Simple producer emitting `ORDER_CREATED`
* Simple consumer reading events
* Basic Kafka configuration

---

### `topics/`

* Create topics using AdminClient
* Configure partitions & replication
* Inspect leader and ISR assignments

---

### `consumer/`

* Multiple consumers in same group
* Observe rebalancing
* Simulate consumer failure
* Verify partition reassignment

---

### `offset/`

* Disable auto-commit
* Commit offsets manually
* Simulate crash before commit
* Replay messages safely

---

### `serialization/`

* Avro producer & consumer
* Schema Registry integration
* Demonstrate schema evolution
* (Optional) Protobuf comparison

---

### `leader/`

Kafka-based leader election using a compacted topic.

Use case:

* Only one instance should:

  * Emit inventory reservation events
  * Run cleanup jobs
  * Trigger compensations

Approach:

* Consumers compete for leadership
* Leader publishes heartbeat
* Failover happens automatically

---

### `ui/`

* Kafka UI via Docker
* Observe:

  * Consumer lag
  * Partition distribution
  * Rebalances
  * Offset commits

---

## 🐳 Infrastructure (Docker)

Docker Compose includes:

* Zookeeper
* Kafka brokers
* Schema Registry
* Kafka UI

```bash
docker-compose up -d
```

---

## 🧪 Experiments to Run

* Add/remove consumers → watch rebalancing
* Kill consumer before offset commit → replay
* Change partition count → observe effects
* Kill leader → observe failover
* Evolve schema → verify compatibility

---

## 🎯 Why Avro (and Not Protobuf)?

| Feature                  | Avro   | Protobuf |
| ------------------------ | ------ | -------- |
| Kafka-native             | ✅      | ❌        |
| Schema Registry          | ✅      | ✅        |
| Evolution-friendly       | ✅      | ⚠️       |
| Learning Kafka internals | ✅ Best | Optional |

**Avro is the primary choice for this repository.**

---

## 🧠 Interview Summary

> “I built a Java-based Kafka system to deeply understand internals like partitioning, ordering guarantees, consumer group coordination, manual offset management, leader election, and schema evolution using Avro.”

---

## 🚀 Next Steps

We will implement this repository **one file at a time**, starting with:

1. Kafka configuration
2. Simple producer
3. Simple consumer
4. Topic creation
5. Consumer groups
6. Manual offset control
7. Avro serialization
8. Leader election

---
