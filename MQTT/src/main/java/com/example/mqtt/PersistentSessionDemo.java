package com.example.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Persistent session (cleanSession=false): while the subscriber is offline the broker queues its
 * QoS 1/2 messages and delivers them when a client with the same ID reconnects.
 */
public final class PersistentSessionDemo {

    private static final String TOPIC = "orders/new";
    private static final String CLIENT_ID = "durable-subscriber";

    private PersistentSessionDemo() {
    }

    public static void run() throws Exception {
        // 1. Connect once with a persistent session and register the subscription, then go offline
        MqttClient subscriber = Config.newClient(CLIENT_ID);
        subscriber.connect(persistentOptions());
        subscriber.subscribe(TOPIC, 1);
        subscriber.disconnect();
        System.out.println("[durable-subscriber] subscribed, now offline");

        // 2. Publish while the subscriber is offline
        MqttClient publisher = Config.connect("order-service");
        for (int i = 1; i <= 3; i++) {
            publisher.publish(TOPIC, Config.bytes("order-" + i), 1, false);
            System.out.println("[order-service] published order-" + i + " (subscriber offline)");
        }
        Config.close(publisher);

        // 3. Reconnect with the same ID: queued messages arrive. The callback must be set before connect().
        CountDownLatch done = new CountDownLatch(3);
        subscriber.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                System.out.println("[durable-subscriber] received after reconnect: " + Config.text(message.getPayload()));
                done.countDown();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
        subscriber.connect(persistentOptions());
        System.out.println("[durable-subscriber] reconnected");

        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Queued messages were not delivered");
        }

        // Clean up: reconnect with cleanSession=true so the broker discards the stored session
        subscriber.disconnect();
        MqttClient cleaner = Config.newClient(CLIENT_ID);
        MqttConnectOptions clean = new MqttConnectOptions();
        clean.setCleanSession(true);
        cleaner.connect(clean);
        Config.close(cleaner);
        subscriber.close();
    }

    private static MqttConnectOptions persistentOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        return options;
    }
}
