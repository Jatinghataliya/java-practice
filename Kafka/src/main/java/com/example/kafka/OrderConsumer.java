package com.example.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

/**
 * Consumes order events. Usage: OrderConsumer [groupId] [consumerName]
 *
 * - Consumers sharing a groupId split the topic's partitions between them.
 * - Consumers in different groups each receive every message independently.
 */
public class OrderConsumer {

    public static void main(String[] args) {
        String groupId = args.length > 0 ? args[0] : KafkaConstants.DEFAULT_GROUP_ID;
        String name = args.length > 1 ? args[1] : "consumer-" + ProcessHandle.current().pid();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // A brand-new group starts from the beginning of the topic.
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Commit manually after processing so a crash never loses a message.
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        // Ctrl+C -> wake up the blocked poll() so we can close cleanly and leave the group.
        Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumer.wakeup();
            try {
                mainThread.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }));

        try {
            consumer.subscribe(List.of(KafkaConstants.TOPIC));
            System.out.printf("[%s] started in group '%s', waiting for messages...%n", name, groupId);

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> r : records) {
                    System.out.printf("[%s|%s] partition=%d offset=%d key=%s value=[%s]%n",
                            groupId, name, r.partition(), r.offset(), r.key(), r.value());
                }
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException expected) {
            // shutting down
        } finally {
            consumer.close();
            System.out.printf("[%s] closed%n", name);
        }
    }
}
