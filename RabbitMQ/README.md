# RabbitMQ demo (core Java, no Spring)

Plain Java with the official `amqp-client` library.

| Demo | Class | Shows |
|---|---|---|
| Work queue | `WorkQueueDemo` | Default exchange, competing workers, manual ack, `basicQos(1)`, persistent messages |
| Pub/Sub | `PubSubDemo` | Fanout exchange, every subscriber queue gets every message |
| Routing | `TopicRoutingDemo` | Topic exchange with `*` and `#` binding patterns |
| Retry + DLQ | `DeadLetterDemo` | `basicNack` with requeue, then dead-letter exchange after 3 attempts |

## Run

```powershell
docker compose up -d
mvn compile exec:java
```

Management UI: http://localhost:15672 (guest/guest)

Connection settings can be overridden with `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`.

Stop the broker with `docker compose down`.
