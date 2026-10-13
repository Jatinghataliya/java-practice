package com.example.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.Random;

/**
 * Produces order events. Usage: OrderProducer [messageCount] [delayMs]
 *
 * The record key is the customer id, so all orders of one customer always land in the
 * same partition (and therefore are consumed in order by the same consumer).
 */
public class OrderProducer {

    public static void main(String[] args) throws Exception {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        long delayMs = args.length > 1 ? Long.parseLong(args[1]) : 500;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // Wait until all in-sync replicas have the record - no data loss if a broker dies.
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        Random random = new Random();
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 1; i <= count; i++) {
                String customer = "customer-" + (1 + random.nextInt(5));
                String value = "orderId=" + i + ",amount=" + (10 + random.nextInt(990));
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(KafkaConstants.TOPIC, customer, value);

                producer.send(record, (meta, ex) -> {
                    if (ex != null) {
                        System.err.println("Send failed: " + ex.getMessage());
                    } else {
                        System.out.printf("Sent key=%s value=[%s] -> partition=%d offset=%d%n",
                                customer, value, meta.partition(), meta.offset());
                    }
                });
                Thread.sleep(delayMs);
            }
            producer.flush();
        }
    }
}
