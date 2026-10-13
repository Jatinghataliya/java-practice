package com.example.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;

import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Creates the "orders" topic (3 partitions, replicated on all 3 brokers) and prints
 * which broker is leader / replica for each partition.
 */
public class TopicCreator {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.BOOTSTRAP_SERVERS);

        try (AdminClient admin = AdminClient.create(props)) {
            Set<String> existing = admin.listTopics().names().get();
            if (!existing.contains(KafkaConstants.TOPIC)) {
                NewTopic topic = new NewTopic(
                        KafkaConstants.TOPIC, KafkaConstants.PARTITIONS, KafkaConstants.REPLICATION_FACTOR);
                admin.createTopics(List.of(topic)).all().get();
                System.out.println("Created topic: " + KafkaConstants.TOPIC);
            } else {
                System.out.println("Topic already exists: " + KafkaConstants.TOPIC);
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
