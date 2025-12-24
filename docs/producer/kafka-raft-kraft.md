# KRaft (Kafka Raft)
**KRaft Mode** (short for Kafka Raft) is the modern consensus mechanism for Apache Kafka that removes its dependency on **Apache ZooKeeper**.

For over a decade, Kafka relied on ZooKeeper to manage its metadata (like topic locations, partition leaders, and cluster membership). KRaft integrates these management tasks directly into Kafka itself, using the **Raft consensus algorithm**.

---

### Why the Change? (The Problem with ZooKeeper)

Before KRaft, every Kafka cluster was actually two separate systems: the Kafka brokers and the ZooKeeper ensemble. This created several "pain points":

* **Double Management:** You had to monitor, secure, and configure two different distributed systems.
* **Scalability Bottlenecks:** ZooKeeper was often the limiting factor when scaling to millions of partitions.
* **Slow Recovery:** When a Kafka controller failed, it had to reload all metadata from ZooKeeper, causing significant downtime for large clusters.

---

### How KRaft Works

In KRaft mode, Kafka replaces ZooKeeper with a **Quorum Controller**.

* **Internal Metadata Topic:** Instead of storing state in an external system, metadata is stored in a special, internal Kafka topic called `__cluster_metadata`.
* **Controller Quorum:** A small set of Kafka nodes are designated as "Controllers." They use the Raft protocol to elect a leader who manages the metadata log and replicates it to the other controllers.
* **Broker Roles:** You can now configure a Kafka node to be a `broker` (handles data), a `controller` (handles metadata), or `both` (combined mode).

---

### Key Benefits of KRaft

| Feature | ZooKeeper Mode | KRaft Mode |
| --- | --- | --- |
| **Complexity** | High (2 separate systems) | Low (1 unified system) |
| **Scalability** | Limited by ZooKeeper | Supports millions of partitions |
| **Failover Time** | Seconds to Minutes | Near-instantaneous |
| **Security** | Requires separate ZK security | Unified Kafka security model |

---

### Is it Production Ready?

**Yes.** * **Kafka 3.3+:** KRaft was declared production-ready.

* **Kafka 4.0 (Released March 2025):** This is a landmark release because it **completely removes ZooKeeper support**. In Kafka 4.x and beyond, KRaft is the *only* way to run Kafka.

To run Kafka in KRaft mode, you need to define a few specific environment variables that tell the broker to act as its own controller and use the Raft protocol instead of looking for ZooKeeper.

### docker-compose.yml
Here is a concise `docker-compose.yml` snippet for a single-node Kafka cluster using KRaft:

```yaml
services:
  kafka:
    image: apache/kafka:latest
    container_name: kafka
    ports:
      - "9092:9092"
    environment:
      # 1. Define the Node ID and roles
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: 'broker,controller'
      
      # 2. Configure the Quorum (Who the controllers are)
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka:9093'
      
      # 3. Listeners: One for clients (9092) and one for KRaft internal (9093)
      KAFKA_LISTENERS: 'PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://localhost:9092'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      
      # 4. Security mapping for the protocols
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT'
      
      # 5. Cluster ID (Required for KRaft to initialize)
      CLUSTER_ID: 'MkU3OEVBNTcwNTJENDM2Qk'

```

---

### Key Variables Explained

* **`KAFKA_PROCESS_ROLES`**: This is the "magic" switch. By setting it to `broker,controller`, you are telling this instance to handle both data (broker) and metadata management (controller).
* **`KAFKA_CONTROLLER_QUORUM_VOTERS`**: This tells Kafka where to find the nodes that vote on metadata. The format is `node_id@host:port`.
* **`CLUSTER_ID`**: KRaft requires a unique UUID to identify the cluster. In a production environment, you would generate this using the `kafka-storage.sh` tool, but for a local Compose file, any base64-encoded string works.

### Why no ZooKeeper?

You'll notice there is no `image: zookeeper` service and no `KAFKA_ZOOKEEPER_CONNECT` variable. The `CONTROLLER` listener on port **9093** handles all the internal communication that ZooKeeper used to manage.