package com.example.activemq;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Point-to-point: each message is delivered to exactly one of the competing consumers. */
public final class QueueDemo {

    private QueueDemo() {
    }

    public static void run(ConnectionFactory factory) throws Exception {
        int messageCount = 6;
        CountDownLatch received = new CountDownLatch(messageCount);

        try (Connection connection = factory.createConnection()) {
            connection.start();

            // Two consumers share the queue
            for (String name : new String[]{"consumer-A", "consumer-B"}) {
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue queue = session.createQueue(Config.ORDERS_QUEUE);
                MessageConsumer consumer = session.createConsumer(queue);
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
            MessageProducer producer = session.createProducer(session.createQueue(Config.ORDERS_QUEUE));
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            for (int i = 1; i <= messageCount; i++) {
                producer.send(session.createTextMessage("order-" + i));
                System.out.println("[producer] sent order-" + i);
            }

            if (!received.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Not all queue messages were received");
            }
        }
    }
}
