package com.example.rabbitmq;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Topic exchange: routing by pattern. '*' matches one word, '#' matches zero or more words.
 * <ul>
 *   <li>errors-consumer binds "*.error"  -> only error logs from any service</li>
 *   <li>orders-consumer binds "orders.#" -> every log from the orders service</li>
 * </ul>
 */
public final class TopicRoutingDemo {

    private TopicRoutingDemo() {
    }

    public static void run(Connection connection) throws Exception {
        // orders.info, orders.error (-> both consumers), payments.error (-> errors), payments.info (-> nobody)
        // errors-consumer: orders.error + payments.error = 2; orders-consumer: orders.info + orders.error = 2
        CountDownLatch done = new CountDownLatch(4);

        try (Channel setup = connection.createChannel()) {
            setup.exchangeDeclare(Config.LOGS_EXCHANGE, BuiltinExchangeType.TOPIC);
        }

        subscribe(connection, "errors-consumer", "*.error", done);
        subscribe(connection, "orders-consumer", "orders.#", done);

        try (Channel publisher = connection.createChannel()) {
            for (String key : new String[]{"orders.info", "orders.error", "payments.error", "payments.info"}) {
                publisher.basicPublish(Config.LOGS_EXCHANGE, key, null, ("log from " + key).getBytes(StandardCharsets.UTF_8));
                System.out.println("[publisher] sent with key " + key);
            }
        }

        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Not all routed messages were received");
        }
    }

    private static void subscribe(Connection connection, String name, String pattern, CountDownLatch done) throws Exception {
        Channel channel = connection.createChannel();
        String queue = channel.queueDeclare().getQueue();
        channel.queueBind(queue, Config.LOGS_EXCHANGE, pattern);
        DeliverCallback onMessage = (tag, delivery) -> {
            System.out.printf("[%s] (%s) got: %s%n", name, delivery.getEnvelope().getRoutingKey(),
                    new String(delivery.getBody(), StandardCharsets.UTF_8));
            done.countDown();
        };
        channel.basicConsume(queue, true, onMessage, tag -> { });
    }
}
