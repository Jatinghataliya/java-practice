# MQTT demo (core Java, no Spring)

Plain Java with the Eclipse Paho MQTT v3 client and an Eclipse Mosquitto broker.

| Demo | Class | Shows |
|---|---|---|
| Pub/Sub | `PubSubDemo` | Topic wildcards `+` (one level) and `#` (all remaining levels) |
| QoS | `QosDemo` | QoS 0 / 1 / 2, and how a QoS 0 subscription downgrades delivery |
| Retained | `RetainedDemo` | Last value stored by the broker and given to late subscribers, clearing it |
| Last Will | `LastWillDemo` | Broker publishes the will message when a client drops without DISCONNECT |
| Persistent session | `PersistentSessionDemo` | `cleanSession=false`: messages sent while offline are delivered on reconnect |

## Run

```powershell
docker compose up -d
mvn compile exec:java
```

Use another broker with `MQTT_BROKER` (default `tcp://localhost:1883`).

Stop the broker with `docker compose down`.

`mosquitto.conf` allows anonymous access without TLS. It is for local demos only.
