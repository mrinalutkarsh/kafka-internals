# [Read EOS before this.](/docs/kafka-eos-exactly-once-semantics.md)

# ❓ Who is committing the offset?

It depends on **which delivery model you are using**.

There are **three distinct cases** in Kafka.

---

## ✅ Case 1: Normal consumer (NO transactions, most common)

### 🔹 Who commits the offset?

👉 **The CONSUMER**

### How?

Either:

* **Auto-commit** (Kafka does it periodically), or
* **Manual commit** (`commitSync / commitAsync`)

### Flow

```
Consumer polls records
   |
   | process record
   |
   | commit offset
   v
__consumer_offsets
```

### Crash scenario

```
poll → process → CRASH before commit ❌
```

Result:

* Offset was **not committed**
* Record will be **reprocessed**
* Possible duplicates ❌

📌 This is **at-least-once semantics**.

---

## ✅ Case 2: Consumer + Producer WITHOUT transactions

(This is the dangerous middle ground)

### Who commits the offset?

👉 **The CONSUMER**

### Who produces output?

👉 **A PRODUCER (separate step)**

### Flow

```
Consumer polls
   |
   | process
   |
   | produce output
   |
   | commit offset
```

### Crash window (classic bug)

```
produce output ✔
CRASH before commit ❌
```

Result:

* Output written
* Offset NOT committed
* Record reprocessed
* Output produced AGAIN ❌❌

📌 This is how duplicates happen in pipelines.

---

## ✅ Case 3: EOS (Exactly-Once Semantics)

This is what you were asking about.

### 🔥 Who commits the offset here?

👉 **The PRODUCER commits the offset**
(not the consumer!)

This is the key insight most people miss.

---

## 🧠 How does that even work?

Kafka allows a **producer** to commit offsets **on behalf of a consumer group**, but **only inside a transaction**.

### API involved

```java
producer.sendOffsetsToTransaction(offsets, consumerGroupId);
```

This sends offsets:

* To `__consumer_offsets`
* As part of the **same transaction** as output records

---

## 🔁 EOS Flow (Very Important)

```
Consumer.poll()
   |
Process records
   |
Producer.send(output)
   |
Producer.sendOffsetsToTransaction()
   |
Producer.commitTransaction()
```

### Result (atomic)

✔ Output visible
✔ Offset committed

OR

❌ Output aborted
❌ Offset NOT committed

Never half-way.

---

## 🔐 Why consumer is NOT allowed to commit offsets here

If the consumer committed offsets:

* It could commit **before** output is durable
* EOS would break

So Kafka enforces:

> Offsets must be committed by the **transaction coordinator (producer)**

---

## 🧾 Where are offsets stored in EOS?

Same place:

```
__consumer_offsets
```

But:

* Written by **transaction coordinator**
* Applied only after transaction commit
* Invisible until commit succeeds

---

## 🧠 Final Truth Table (Memorize This)

| Mode          | Who commits offset | Semantics     |
| ------------- | ------------------ | ------------- |
| Auto-commit   | Consumer           | At-most-once  |
| Manual commit | Consumer           | At-least-once |
| EOS           | **Producer**       | Exactly-once  |

---

## 🎯 Interview-Grade One-Liner

> In normal Kafka consumption, offsets are committed by the consumer, but under exactly-once semantics the producer commits offsets as part of the transaction using `sendOffsetsToTransaction`.

---

## 🧠 Why your confusion was valid

Because:

* Offsets conceptually belong to consumers
* But **ownership of commit** changes in EOS
* Kafka bends the rules to guarantee atomicity

---

## ✅ Short answer (what actually happens in production)

👉 **Manual commit by consumers is by far the most common in production.**
👉 **EOS is used only in specific, high-value pipelines.**

Most real systems **do NOT use EOS**.

---

## 📊 Reality Check (Industry Practice)

### 1️⃣ Manual commit (At-least-once) — **~80–90% of production systems**

This is the **default choice**.

**Why it dominates:**

* Simple mental model
* Works with databases, caches, HTTP calls
* No transactions complexity
* Easy to debug and operate

**Typical pattern:**

```
poll → process → write to DB → commit offset
```

Duplicates are handled by:

* idempotent DB writes
* unique constraints
* dedup keys

📌 This is the **most common production setup**.

---

### 2️⃣ EOS (Exactly-Once) — **~5–10% of systems**

Used when:

* Kafka → Kafka pipelines
* Financial / ledger systems
* Stream processing (Kafka Streams)
* Strict correctness > simplicity

**Examples:**

* Kafka Streams apps
* Bank transaction processing
* Stateful aggregations

EOS is **not free**:

* More configs
* More failure modes
* Higher latency
* Harder debugging

---

### 3️⃣ Auto-commit — **Almost never used**

Seen only in:

* POCs
* Metrics/log scraping
* Fire-and-forget consumers

In serious systems:
❌ Auto-commit is avoided

---

## 🧠 Why manual commit wins in practice

### EOS only works **fully** when:

* Input = Kafka
* Output = Kafka

The moment you add:

* Database
* REST call
* External system

Kafka **cannot** guarantee exactly-once anymore.

So teams prefer:

```
At-least-once + idempotent processing
```

This scales to **any sink**.

---

## 🧱 Decision Matrix (Memorize This)

| Scenario      | Best Choice    |
| ------------- | -------------- |
| Kafka → Kafka | EOS            |
| Kafka → DB    | Manual commit  |
| Kafka → API   | Manual commit  |
| Kafka Streams | EOS (built-in) |
| Log ingestion | Manual commit  |
| Analytics     | Manual commit  |

---

## 🎯 Interview-Grade Answer

> In most production systems, consumers use manual offset commits with at-least-once semantics. Exactly-once semantics are used only in specific Kafka-to-Kafka or stream-processing pipelines where strict atomicity is required.

---

## 🧠 What Interviewers Really Want to Hear

Not:

> “EOS is better”

But:

> “EOS is powerful but complex, and most production systems prefer at-least-once with idempotent processing unless strict guarantees are required.”

That shows **engineering judgment**.

---
