package com.example.kafka;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Kafka Streams computations on the "orders" topic (value format: "orderId=1,amount=202").
 *
 *  1. running TOTAL amount per customer          -> order-total-by-customer
 *  2. running AVERAGE amount per customer        -> order-avg-by-customer
 *  3. TOTAL + COUNT per customer per 10s window  -> order-windowed-total
 *  4. FILTER of high value orders (>= 500)       -> high-value-orders
 *
 * Run several instances with the same application.id and Kafka splits the 3 partitions between
 * them (state stores are backed up in changelog topics, so a restarted instance recovers).
 */
public class OrderStreamsApp {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-streams-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.BOOTSTRAP_SERVERS);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        // Internal changelog/repartition topics must also be replicated on the 3 brokers.
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 3);
        // Demo settings: emit every update immediately instead of batching in the record cache.
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 500);
        props.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 0);

        Topology topology = buildTopology();
        System.out.println(topology.describe());

        KafkaStreams streams = new KafkaStreams(topology, props);
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            streams.close(Duration.ofSeconds(10));
            latch.countDown();
        }));

        streams.start();
        System.out.println("Kafka Streams started, reading topic '" + KafkaConstants.TOPIC + "' ...");
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> orders = builder.stream(
                KafkaConstants.TOPIC, Consumed.with(Serdes.String(), Serdes.String()));

        // 1. Running total per customer (stateful aggregation -> KTable)
        orders.groupByKey()
                .aggregate(
                        () -> 0L,
                        (customer, order, total) -> total + amountOf(order),
                        Materialized.with(Serdes.String(), Serdes.Long()))
                .toStream()
                .mapValues(total -> "total=" + total)
                .to(KafkaConstants.TOTAL_BY_CUSTOMER_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        // 2. Running average per customer. The aggregate is kept as "count:sum" text so no
        //    custom serde is needed; the average is derived when writing to the output topic.
        orders.groupByKey()
                .aggregate(
                        () -> "0:0",
                        (customer, order, agg) -> {
                            String[] parts = agg.split(":");
                            long count = Long.parseLong(parts[0]) + 1;
                            long sum = Long.parseLong(parts[1]) + amountOf(order);
                            return count + ":" + sum;
                        },
                        Materialized.with(Serdes.String(), Serdes.String()))
                .toStream()
                .mapValues(agg -> {
                    String[] parts = agg.split(":");
                    long count = Long.parseLong(parts[0]);
                    long sum = Long.parseLong(parts[1]);
                    return String.format("count=%d avg=%.2f", count, (double) sum / count);
                })
                .to(KafkaConstants.AVG_BY_CUSTOMER_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        // 3. Total + count per customer in 10 second tumbling windows (event-time based)
        orders.groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(10)))
                .aggregate(
                        () -> "0:0",
                        (customer, order, agg) -> {
                            String[] parts = agg.split(":");
                            return (Long.parseLong(parts[0]) + 1) + ":" + (Long.parseLong(parts[1]) + amountOf(order));
                        },
                        Materialized.with(Serdes.String(), Serdes.String()))
                .toStream()
                .map((windowedKey, agg) -> {
                    String[] parts = agg.split(":");
                    String key = windowedKey.key() + "@[" + windowedKey.window().startTime().toString().substring(11, 19)
                            + " - " + windowedKey.window().endTime().toString().substring(11, 19) + "]";
                    return org.apache.kafka.streams.KeyValue.pair(key, "count=" + parts[0] + " total=" + parts[1]);
                })
                .to(KafkaConstants.WINDOWED_TOTAL_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        // 4. Stateless filter: copy high value orders to their own topic
        orders.filter((customer, order) -> amountOf(order) >= KafkaConstants.HIGH_VALUE_THRESHOLD)
                .to(KafkaConstants.HIGH_VALUE_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        return builder.build();
    }

    /** Extracts the amount from "orderId=1,amount=202". */
    static long amountOf(String order) {
        int idx = order.indexOf("amount=");
        return Long.parseLong(order.substring(idx + "amount=".length()).trim());
    }
}
