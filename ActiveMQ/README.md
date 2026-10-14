# ActiveMQ demo (core Java, no Spring)

Plain JMS (`javax.jms`) with Apache ActiveMQ Classic 5.18.x.

| Demo | Class | Shows |
|---|---|---|
| Queue | `QueueDemo` | Point-to-point, two competing consumers share the messages |
| Topic | `TopicDemo` | Publish/subscribe, every subscriber gets every message |
| Retry + DLQ | `DeadLetterDemo` | Transacted rollback, redelivery policy, move to `ActiveMQ.DLQ` |

## Run

```powershell
mvn compile exec:java
```

By default an embedded, non-persistent broker starts on `tcp://localhost:61616`.

To use a real broker instead:

```powershell
docker compose up -d
$env:ACTIVEMQ_URL = "tcp://localhost:61616"
mvn compile exec:java
```

Web console: http://localhost:8161 (admin/admin)
