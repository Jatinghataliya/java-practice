package com.example.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public final class Config {

    public static final String TASKS_QUEUE = "tasks.queue";
    public static final String NEWS_EXCHANGE = "news.exchange";
    public static final String LOGS_EXCHANGE = "logs.exchange";
    public static final String PAYMENTS_QUEUE = "payments.queue";
    public static final String DEAD_LETTER_EXCHANGE = "payments.dlx";
    public static final String DEAD_LETTER_QUEUE = "payments.dlq";

    private Config() {
    }

    public static Connection connect() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(env("RABBITMQ_HOST", "localhost"));
        factory.setPort(Integer.parseInt(env("RABBITMQ_PORT", "5672")));
        factory.setUsername(env("RABBITMQ_USER", "guest"));
        factory.setPassword(env("RABBITMQ_PASSWORD", "guest"));
        return factory.newConnection("rabbitmq-demo");
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
