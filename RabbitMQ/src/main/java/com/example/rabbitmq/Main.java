package com.example.rabbitmq;

import com.rabbitmq.client.Connection;

public final class Main {

    public static void main(String[] args) throws Exception {
        try (Connection connection = Config.connect()) {
            section("1. Work queue (competing consumers, manual ack)");
            WorkQueueDemo.run(connection);

            section("2. Pub/Sub (fanout exchange)");
            PubSubDemo.run(connection);

            section("3. Routing (topic exchange)");
            TopicRoutingDemo.run(connection);

            section("4. Retry + Dead-letter exchange");
            DeadLetterDemo.run(connection);

            System.out.println("\nAll demos completed.");
        }
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
