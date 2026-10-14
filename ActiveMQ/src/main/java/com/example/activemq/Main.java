package com.example.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;

public final class Main {

    public static void main(String[] args) throws Exception {
        String external = Config.externalBrokerUrl();
        EmbeddedBroker embedded = external == null ? new EmbeddedBroker() : null;
        String url = external != null ? external : Config.BROKER_URL;
        System.out.println("Using broker: " + url + (embedded != null ? " (embedded)" : " (external)"));

        try {
            ActiveMQConnectionFactory factory = Config.connectionFactory(url);

            section("1. Queue (point-to-point)");
            QueueDemo.run(factory);

            section("2. Topic (publish/subscribe)");
            TopicDemo.run(factory);

            section("3. Retry + Dead-letter queue");
            DeadLetterDemo.run(factory);

            System.out.println("\nAll demos completed.");
        } finally {
            if (embedded != null) {
                embedded.close();
            }
        }
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
