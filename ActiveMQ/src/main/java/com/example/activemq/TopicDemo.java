package com.example.activemq;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Publish/subscribe: every active subscriber receives every message. */
public final class TopicDemo {

    private TopicDemo() {
    }

    public static void run(ConnectionFactory factory) throws Exception {
        int messageCount = 3;
        int subscribers = 2;
        CountDownLatch received = new CountDownLatch(messageCount * subscribers);

        try (Connection connection = factory.createConnection()) {
            connection.start();

            // Subscribers must exist before publishing (non-durable subscriptions)
            for (int s = 1; s <= subscribers; s++) {
                String name = "subscriber-" + s;
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Topic topic = session.createTopic(Config.NEWS_TOPIC);
                MessageConsumer consumer = session.createConsumer(topic);
                consumer.setMessageListener(message -> {
                    try {
                        System.out.printf("[%s] got: %s%n", name, ((TextMessage) message).getText());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        received.countDown();
                    }
                });
            }

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer publisher = session.createProducer(session.createTopic(Config.NEWS_TOPIC));
            for (int i = 1; i <= messageCount; i++) {
                publisher.send(session.createTextMessage("news-" + i));
                System.out.println("[publisher] sent news-" + i);
            }

            if (!received.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Not all topic messages were received");
            }
        }
    }
}
