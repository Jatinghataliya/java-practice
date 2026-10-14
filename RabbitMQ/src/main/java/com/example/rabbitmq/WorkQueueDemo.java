package com.example.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Work queue: messages are shared round-robin between competing workers, with manual acks. */
public final class WorkQueueDemo {

    private WorkQueueDemo() {
    }

    public static void run(Connection connection) throws Exception {
        int messageCount = 6;
        CountDownLatch done = new CountDownLatch(messageCount);

        try (Channel setup = connection.createChannel()) {
            setup.queueDeclare(Config.TASKS_QUEUE, true, false, false, null);
            setup.queuePurge(Config.TASKS_QUEUE);
        }

        // One channel per worker; prefetch=1 means a worker gets a new task only after acking the last one
        for (String name : new String[]{"worker-A", "worker-B"}) {
            Channel channel = connection.createChannel();
            channel.basicQos(1);
            DeliverCallback onMessage = (tag, delivery) -> {
                String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.printf("[%s] got: %s%n", name, body);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                done.countDown();
            };
            channel.basicConsume(Config.TASKS_QUEUE, false, onMessage, tag -> { });
        }

        try (Channel producer = connection.createChannel()) {
            for (int i = 1; i <= messageCount; i++) {
                String body = "task-" + i;
                // Default exchange: routing key = queue name. PERSISTENT_TEXT_PLAIN survives a broker restart.
                producer.basicPublish("", Config.TASKS_QUEUE, MessageProperties.PERSISTENT_TEXT_PLAIN,
                        body.getBytes(StandardCharsets.UTF_8));
                System.out.println("[producer] sent " + body);
            }
        }

        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Not all tasks were processed");
        }
    }
}
