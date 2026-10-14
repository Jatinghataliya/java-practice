package com.example.activemq;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Failure handling: a transacted consumer rolls back on error. ActiveMQ redelivers the message
 * {@link Config#MAX_REDELIVERIES} times, then moves it to the dead-letter queue.
 */
public final class DeadLetterDemo {

    private DeadLetterDemo() {
    }

    public static void run(ConnectionFactory factory) throws Exception {
        String queueName = "payments.queue";

        try (Connection connection = factory.createConnection()) {
            connection.start();

            Session producerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = producerSession.createProducer(producerSession.createQueue(queueName));
            producer.send(producerSession.createTextMessage("good-payment"));
            producer.send(producerSession.createTextMessage("poison-payment"));

            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            MessageConsumer consumer = session.createConsumer(session.createQueue(queueName));
            AtomicInteger poisonAttempts = new AtomicInteger();

            // 1 good message + (1 original + MAX_REDELIVERIES) attempts at the poison message
            int expectedDeliveries = 1 + 1 + Config.MAX_REDELIVERIES;
            for (int i = 0; i < expectedDeliveries; i++) {
                Message message = consumer.receive(5000);
                if (message == null) {
                    throw new IllegalStateException("Expected a delivery but none arrived");
                }
                String text = ((TextMessage) message).getText();
                if (text.startsWith("poison")) {
                    System.out.printf("[consumer] attempt %d failed for %s -> rollback%n",
                            poisonAttempts.incrementAndGet(), text);
                    session.rollback();
                } else {
                    System.out.println("[consumer] processed " + text + " -> commit");
                    session.commit();
                }
            }

            // The poison message is now in the DLQ
            Session dlqSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer dlqConsumer = dlqSession.createConsumer(dlqSession.createQueue(Config.DLQ));
            Message dead = dlqConsumer.receive(5000);
            if (dead == null) {
                throw new IllegalStateException("Message was not moved to the DLQ");
            }
            System.out.println("[dlq] dead-lettered: " + ((TextMessage) dead).getText());
        }
    }
}
