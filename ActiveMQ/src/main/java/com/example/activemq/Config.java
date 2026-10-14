package com.example.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;

public final class Config {

    public static final String BROKER_URL = "tcp://localhost:61616";
    public static final String ORDERS_QUEUE = "orders.queue";
    public static final String NEWS_TOPIC = "news.topic";
    public static final String DLQ = "ActiveMQ.DLQ";
    public static final int MAX_REDELIVERIES = 2;

    private Config() {
    }

    /** External broker URL from env ACTIVEMQ_URL, or null to use the embedded broker. */
    public static String externalBrokerUrl() {
        String url = System.getenv("ACTIVEMQ_URL");
        return (url == null || url.isBlank()) ? null : url;
    }

    public static ActiveMQConnectionFactory connectionFactory(String url) {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);
        // Used by transacted/client-ack sessions: after MAX_REDELIVERIES the broker moves the message to the DLQ.
        RedeliveryPolicy policy = new RedeliveryPolicy();
        policy.setMaximumRedeliveries(MAX_REDELIVERIES);
        policy.setInitialRedeliveryDelay(200);
        policy.setRedeliveryDelay(200);
        factory.setRedeliveryPolicy(policy);
        return factory;
    }
}
