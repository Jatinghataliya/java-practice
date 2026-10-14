package com.example.activemq;

import org.apache.activemq.broker.BrokerService;

/** Minimal in-JVM, non-persistent ActiveMQ broker listening on {@link Config#BROKER_URL}. */
public final class EmbeddedBroker implements AutoCloseable {

    private final BrokerService broker = new BrokerService();

    public EmbeddedBroker() throws Exception {
        broker.setBrokerName("demo-broker");
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.addConnector(Config.BROKER_URL);
        broker.start();
        broker.waitUntilStarted();
    }

    @Override
    public void close() throws Exception {
        broker.stop();
        broker.waitUntilStopped();
    }
}
