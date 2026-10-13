package com.example.kafka;

public final class KafkaConstants {

    private KafkaConstants() {
    }

    /** All three brokers - the client only needs one to bootstrap, the rest is discovered. */
    public static final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9093,localhost:9094";

    public static final String TOPIC = "orders";
    public static final int PARTITIONS = 3;
    public static final short REPLICATION_FACTOR = 3;

    public static final String DEFAULT_GROUP_ID = "order-processors";
}
