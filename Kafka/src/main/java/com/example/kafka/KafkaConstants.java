package com.example.kafka;

import java.util.List;

public final class KafkaConstants {

    private KafkaConstants() {
    }

    /** All three brokers - the client only needs one to bootstrap, the rest is discovered. */
    public static final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9093,localhost:9094";

    public static final String TOPIC = "orders";
    public static final int PARTITIONS = 3;
    public static final short REPLICATION_FACTOR = 3;

    public static final String DEFAULT_GROUP_ID = "order-processors";

    // Kafka Streams output topics
    public static final String TOTAL_BY_CUSTOMER_TOPIC = "order-total-by-customer";
    public static final String AVG_BY_CUSTOMER_TOPIC = "order-avg-by-customer";
    public static final String WINDOWED_TOTAL_TOPIC = "order-windowed-total";
    public static final String HIGH_VALUE_TOPIC = "high-value-orders";
    public static final List<String> OUTPUT_TOPICS = List.of(
            TOTAL_BY_CUSTOMER_TOPIC, AVG_BY_CUSTOMER_TOPIC, WINDOWED_TOTAL_TOPIC, HIGH_VALUE_TOPIC);

    /** Orders at or above this amount are copied to HIGH_VALUE_TOPIC. */
    public static final int HIGH_VALUE_THRESHOLD = 500;
}
