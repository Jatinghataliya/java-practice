# Kafka with Core Java

Plain Java (no Spring) example of a Kafka producer and consumer group against a
**3-broker cluster managed by ZooKeeper**.

```
                +-----------+
                | ZooKeeper |  (broker registry, controller election)
                +-----+-----+
          +-----------+-----------+
     +----+----+ +----+----+ +----+----+
     | kafka1  | | kafka2  | | kafka3  |   localhost:9092 / 9093 / 9094
     +---------+ +---------+ +---------+
        topic "orders": 3 partitions, replication factor 3

 OrderProducer ---> orders ---> consumer group "order-processors" (c1, c2, c3 share partitions)
                           ---> consumer group "audit"            (gets every message too)
```

## Files

| File | Purpose |
|------|---------|
| `docker-compose.yml` | ZooKeeper + 3 Kafka brokers |
| `TopicCreator` | Creates `orders` topic (3 partitions, RF 3) and prints leader/replicas/ISR |
| `OrderProducer` | Sends keyed messages (`acks=all`, idempotent) |
| `OrderConsumer` | Consumer in a group, manual offset commit, clean shutdown |
| `OrderStreamsApp` | **Kafka Streams** computations on `orders` (see below) |
| `StreamsResultPrinter` | Prints everything the streams app writes to its output topics |

## Run

```powershell
# 1. start the cluster
docker compose up -d

# 2. create the topic (wait ~20s for brokers to be ready)
mvn -q compile exec:java "-Dexec.mainClass=com.example.kafka.TopicCreator"

# 3. start consumers - run each in its own terminal (same group => partitions are split)
mvn -q compile exec:java "-Dexec.mainClass=com.example.kafka.OrderConsumer" "-Dexec.args=order-processors c1"
mvn -q compile exec:java "-Dexec.mainClass=com.example.kafka.OrderConsumer" "-Dexec.args=order-processors c2"

# 4. a consumer in a different group receives ALL messages independently
mvn -q compile exec:java "-Dexec.mainClass=com.example.kafka.OrderConsumer" "-Dexec.args=audit a1"

# 5. produce 20 messages, 500 ms apart
mvn -q compile exec:java "-Dexec.mainClass=com.example.kafka.OrderProducer" "-Dexec.args=20 500"
```

## Kafka Streams computations

`OrderStreamsApp` builds one topology that reads `orders` and produces four output streams in parallel:

| Output topic | Computation | Example output |
|---|---|---|
| `order-total-by-customer` | Running total per customer | `customer-2 → total=3706` |
| `order-avg-by-customer` | Running average per customer | `customer-2 → count=6 avg=445.50` |
| `order-windowed-total` | Total + count per customer per **10 second tumbling window** | `customer-2@[06:17:40-06:17:50] → count=6 total=2673` |
| `high-value-orders` | Stateless filter – orders ≥ 500 copied as-is | `customer-1 → orderId=8,amount=716` |

```powershell
# In terminal 1 – start the streams app (Ctrl+C to stop)
mvn -q compile exec:java "-Dexec.mainClass=com.example.kafka.OrderStreamsApp"

# In terminal 2 – watch all 4 output topics
mvn -q compile exec:java "-Dexec.mainClass=com.example.kafka.StreamsResultPrinter"

# In terminal 3 – produce orders
mvn -q compile exec:java "-Dexec.mainClass=com.example.kafka.OrderProducer" "-Dexec.args=20 300"
```

**Scaling:** start a second `OrderStreamsApp` with the same `application.id`.
Kafka assigns different partitions to each instance and they share the work.

## Things to try

- **Consumer group rebalancing:** stop `c1` (Ctrl+C) - its partitions move to `c2`.
- **More consumers than partitions:** a 4th consumer in `order-processors` stays idle (3 partitions).
- **Ordering:** the key is the customer id, so one customer's orders always go to the same partition, in order.
- **Broker failure:** `docker stop kafka1` - leaders move to the other brokers, producer/consumers keep working
  (re-run `TopicCreator` to see the new leaders / ISR).
- **ZooKeeper view:** `docker exec zookeeper zookeeper-shell localhost:2181 ls /brokers/ids`
- **Group state:** `docker exec kafka1 kafka-consumer-groups --bootstrap-server kafka1:29092 --describe --group order-processors`

## Stop

```powershell
docker compose down -v
```
