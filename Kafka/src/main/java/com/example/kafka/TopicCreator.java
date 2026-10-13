package com.example.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Creates the "orders" topic plus the Kafka Streams output topics (3 partitions, replicated on
 * all 3 brokers) and prints which broker is leader / replica for each partition of "orders".
 */
public class TopicCreator {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.BOOTSTRAP_SERVERS);

        List<String> allTopics = new ArrayList<>();
        allTopics.add(KafkaConstants.TOPIC);
        allTopics.addAll(KafkaConstants.OUTPUT_TOPICS);

        try (AdminClient admin = AdminClient.create(props)) {
            Set<String> existing = admin.listTopics().names().get();
            for (String name : allTopics) {
                if (!existing.contains(name)) {
                    NewTopic topic = new NewTopic(
                            name, KafkaConstants.PARTITIONS, KafkaConstants.REPLICATION_FACTOR);
                    admin.createTopics(List.of(topic)).all().get();
                    System.out.println("Created topic: " + name);
                } else {
                    System.out.println("Topic already exists: " + name);
                }
            }

            TopicDescription description = admin.describeTopics(List.of(KafkaConstants.TOPIC))
                    .allTopicNames().get().get(KafkaConstants.TOPIC);
            description.partitions().forEach(p -> System.out.printf(
                    "partition=%d leader=broker-%d replicas=%s isr=%s%n",
                    p.partition(), p.leader().id(),
                    p.replicas().stream().map(n -> n.id()).toList(),
                    p.isr().stream().map(n -> n.id()).toList()));
        }
    }
}
