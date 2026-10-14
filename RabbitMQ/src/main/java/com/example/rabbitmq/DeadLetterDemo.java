package com.example.rabbitmq;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.GetResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Retry + dead-letter exchange. The main queue is configured with a DLX. A failing message is requeued
 * until MAX_ATTEMPTS is reached, then rejected with requeue=false, so the broker routes it to the DLQ.
 */
public final class DeadLetterDemo {

    private static final int MAX_ATTEMPTS = 3;

    private DeadLetterDemo() {
    }

    public static void run(Connection connection) throws Exception {
        try (Channel setup = connection.createChannel()) {
            setup.exchangeDeclare(Config.DEAD_LETTER_EXCHANGE, BuiltinExchangeType.DIRECT);
            setup.queueDeclare(Config.DEAD_LETTER_QUEUE, true, false, false, null);
            setup.queueBind(Config.DEAD_LETTER_QUEUE, Config.DEAD_LETTER_EXCHANGE, Config.PAYMENTS_QUEUE);

            Map<String, Object> args = new HashMap<>();
            args.put("x-dead-letter-exchange", Config.DEAD_LETTER_EXCHANGE);
            // Dead-lettered messages keep their original routing key unless overridden
            args.put("x-dead-letter-routing-key", Config.PAYMENTS_QUEUE);
            setup.queueDeclare(Config.PAYMENTS_QUEUE, true, false, false, args);
            setup.queuePurge(Config.PAYMENTS_QUEUE);
            setup.queuePurge(Config.DEAD_LETTER_QUEUE);
        }

        // 1 good message processed once + MAX_ATTEMPTS deliveries of the poison message
        CountDownLatch done = new CountDownLatch(1 + MAX_ATTEMPTS);
        AtomicInteger poisonAttempts = new AtomicInteger();

        Channel consumer = connection.createChannel();
        DeliverCallback onMessage = (tag, delivery) -> {
            long deliveryTag = delivery.getEnvelope().getDeliveryTag();
            String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
            if (body.startsWith("poison")) {
                int attempt = poisonAttempts.incrementAndGet();
                boolean requeue = attempt < MAX_ATTEMPTS;
                System.out.printf("[consumer] attempt %d failed for %s -> %s%n", attempt, body,
                        requeue ? "requeue" : "reject (to DLQ)");
                consumer.basicNack(deliveryTag, false, requeue);
            } else {
                System.out.println("[consumer] processed " + body + " -> ack");
                consumer.basicAck(deliveryTag, false);
            }
            done.countDown();
        };

        try (Channel producer = connection.createChannel()) {
            producer.basicPublish("", Config.PAYMENTS_QUEUE, null, "good-payment".getBytes(StandardCharsets.UTF_8));
            producer.basicPublish("", Config.PAYMENTS_QUEUE, null, "poison-payment".getBytes(StandardCharsets.UTF_8));
        }
        consumer.basicConsume(Config.PAYMENTS_QUEUE, false, onMessage, tag -> { });

        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Messages were not processed in time");
        }

        try (Channel dlq = connection.createChannel()) {
            GetResponse dead = null;
            for (int i = 0; i < 20 && dead == null; i++) {
                dead = dlq.basicGet(Config.DEAD_LETTER_QUEUE, true);
                if (dead == null) {
                    Thread.sleep(100);
                }
            }
            if (dead == null) {
                throw new IllegalStateException("Message was not moved to the DLQ");
            }
            System.out.println("[dlq] dead-lettered: " + new String(dead.getBody(), StandardCharsets.UTF_8));
        }
        consumer.close();
    }
}
