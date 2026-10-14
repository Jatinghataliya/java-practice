package com.example.rabbitmq;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Fanout exchange: every bound queue (subscriber) receives a copy of every message. */
public final class PubSubDemo {

    private PubSubDemo() {
    }

    public static void run(Connection connection) throws Exception {
        int messageCount = 3;
        int subscribers = 2;
        CountDownLatch done = new CountDownLatch(messageCount * subscribers);

        try (Channel setup = connection.createChannel()) {
            setup.exchangeDeclare(Config.NEWS_EXCHANGE, BuiltinExchangeType.FANOUT);
        }

        for (int s = 1; s <= subscribers; s++) {
            String name = "subscriber-" + s;
            Channel channel = connection.createChannel();
            // Server-named, exclusive, auto-delete queue: lives only as long as this subscriber
            String queue = channel.queueDeclare().getQueue();
            channel.queueBind(queue, Config.NEWS_EXCHANGE, "");
            DeliverCallback onMessage = (tag, delivery) -> {
                System.out.printf("[%s] got: %s%n", name, new String(delivery.getBody(), StandardCharsets.UTF_8));
                done.countDown();
            };
            channel.basicConsume(queue, true, onMessage, tag -> { });
        }

        try (Channel publisher = connection.createChannel()) {
            for (int i = 1; i <= messageCount; i++) {
                String body = "news-" + i;
                publisher.basicPublish(Config.NEWS_EXCHANGE, "", null, body.getBytes(StandardCharsets.UTF_8));
                System.out.println("[publisher] sent " + body);
            }
        }

        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Not all news messages were received");
        }
    }
}
