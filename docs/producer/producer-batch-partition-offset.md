# 1️⃣ How does Kafka producer batching actually work?

You wrote:

```java
producer.send(record, callback);
```

You were told:

> Kafka batches internally

Now let’s unpack **how**.

---

## 🔧 Producer Architecture (Internals)

When you call `send()`:

```
Your Thread
   |
   v
KafkaProducer.send()
   |
   v
RecordAccumulator  <-- batching happens HERE
   |
   v
Sender Thread (background)
   |
   v
Broker (Leader Partition)
```

### ❗ Important

* `send()` **does NOT send over the network**
* It **adds the record to an in-memory buffer**

---

## 🧱 RecordAccumulator (The Batch Builder)

Kafka maintains **one batch per partition**:

```
Topic: order-events

Partition 0 -> Batch A
Partition 1 -> Batch B
Partition 2 -> Batch C
```

When you send records:

```java
send(orderId=123)
send(orderId=456)
```

Kafka does:

1. Hash key → decide partition
2. Append record to that partition’s batch
3. Return immediately

---

## ⏱️ When does Kafka send the batch?

A batch is sent when **ANY** of these happens:

### ✅ 1. `batch.size` is reached (default ~16 KB)

```
Batch full → send immediately
```

---

### ✅ 2. `linger.ms` expires

From your config:

```java
linger.ms = 5
```

Meaning:

> “Wait up to 5ms for more records before sending”

This allows **more records to join the batch**, improving throughput.

---

### ✅ 3. `flush()` is called

```java
producer.flush();
```

Force-send **everything**, regardless of size or time.

---

### ✅ 4. Memory pressure

If buffers are full:

* Sender thread wakes up
* Sends partial batches

---

## 🧠 Why batching matters

| Without batching        | With batching                   |
| ----------------------- | ------------------------------- |
| 1 network call / record | 1 network call / batch          |
| Very slow               | Very fast                       |
| High latency            | Slight latency, huge throughput |

Kafka is optimized for **throughput**, not chatty sends.

---

# 2️⃣ Who sets the partition and offset?

You saw output like:

```
partition=2 | offset=17
```

Let’s separate **partition choice** and **offset assignment**.

---

## 🧭 Who decides the PARTITION?

### Answer: **The producer client**

Specifically:

```
KafkaProducer → Partitioner
```

### Default behavior:

```text
if key != null:
    partition = hash(key) % numPartitions
else:
    round-robin
```

So in your case:

```java
new ProducerRecord<>(topic, orderId, value)
```

* `orderId` is hashed
* Partition is chosen **before network call**
* This ensures:

  * Same key → same partition
  * Ordering guarantee

📌 **Broker does NOT choose partition**

---

## 📍 Who assigns the OFFSET?

### Answer: **The broker (leader of that partition)**

This is critical.

### Why producers cannot assign offsets?

Because:

* Multiple producers may write concurrently
* Only the leader broker has the authoritative log

---

## 🧱 Internals: Partition Log

Each partition is:

```
Append-only log
-------------------------
offset | message
-------------------------
0      | event A
1      | event B
2      | event C
...
17     | your event
```

When the broker receives a batch:

1. Appends records to the log
2. Assigns offsets sequentially
3. Replicates to ISR followers
4. Acknowledges producer

📌 Offset is:

* **Per partition**
* **Monotonically increasing**
* **Never reused**

---

## 🧠 Summary So Far

| Thing      | Who decides            |
| ---------- | ---------------------- |
| Partition  | Producer (Partitioner) |
| Offset     | Broker (Leader)        |
| Ordering   | Partition log          |
| Durability | ISR + acks config      |

---

# 3️⃣ When is the callback invoked, and who invokes it?

You asked:

> when is callback invoked who is invoking it?

This is a **great question**.

---

## 🔁 End-to-End Timeline

```
Your Thread
   |
   | send(record, callback)
   v
RecordAccumulator
   |
   | (background)
   v
Sender Thread
   |
   | network request
   v
Leader Broker
   |
   | append + replicate
   v
Broker sends response
   |
   v
Sender Thread receives response
   |
   v
Callback.onCompletion()
```

---

## 🧠 Key Points

### ❌ Callback is NOT invoked by:

* Your thread
* Broker
* Zookeeper

### ✅ Callback IS invoked by:

> **Kafka Producer’s internal sender thread**

---

## ⏱️ When exactly is callback invoked?

Depends on `acks`:

### With `acks=all` (your config):

Callback fires **only after**:

1. Leader appends record
2. All ISR replicas confirm write
3. Broker sends success response

This is the **strongest durability guarantee**.

---

## ❗ Important Subtlety

Callbacks:

* Are invoked **asynchronously**
* Can be invoked **out of order across partitions**
* Are ordered **within the same partition**

---

## ⚠️ Production Warning (Why this matters)

**DO NOT do heavy work inside callback**:

❌ DB calls
❌ Blocking operations

Because:

* Callback runs on Kafka sender thread
* Blocking it slows ALL sends

---

# 🧠 Mental Model (Memorize This)

```
Producer.send()
  → buffer (RecordAccumulator)
    → batch (per partition)
      → sender thread
        → leader broker
          → append + replicate
            → response
              → callback
```

---

# 🔥 One-Line Interview Answers

**Q: How does Kafka batching work?**

> Kafka buffers records per partition in memory and sends them as batches based on size, time (`linger.ms`), or flush.

**Q: Who assigns partitions and offsets?**

> The producer chooses the partition using a partitioner; the broker assigns offsets when appending to the partition log.

**Q: Who invokes the producer callback?**

> Kafka’s internal sender thread invokes the callback after receiving broker acknowledgment.

---
