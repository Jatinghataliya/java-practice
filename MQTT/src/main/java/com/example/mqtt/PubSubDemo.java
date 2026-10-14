package com.example.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Topic wildcards: '+' matches exactly one level, '#' matches all remaining levels.
 * <ul>
 *   <li>temp-dashboard subscribes "home/+/temp"</li>
 *   <li>everything-logger subscribes "home/#"</li>
 * </ul>
 */
public final class PubSubDemo {

    private PubSubDemo() {
    }

    public static void run() throws Exception {
        // temp-dashboard: room1/temp + room2/temp = 2; everything-logger: all 3 messages
        CountDownLatch done = new CountDownLatch(5);

        MqttClient dashboard = Config.connect("temp-dashboard");
        dashboard.subscribe("home/+/temp", 1, (topic, message) -> {
            System.out.printf("[temp-dashboard] %s = %s%n", topic, Config.text(message.getPayload()));
            done.countDown();
        });

        MqttClient logger = Config.connect("everything-logger");
        logger.subscribe("home/#", 1, (topic, message) -> {
            System.out.printf("[everything-logger] %s = %s%n", topic, Config.text(message.getPayload()));
            done.countDown();
        });

        MqttClient sensor = Config.connect("sensor");
        publish(sensor, "home/room1/temp", "21.5");
        publish(sensor, "home/room2/temp", "19.0");
        publish(sensor, "home/door", "OPEN");

        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Not all messages were received");
        }
        Config.close(sensor);
        Config.close(dashboard);
        Config.close(logger);
    }

    private static void publish(MqttClient client, String topic, String payload) throws Exception {
        client.publish(topic, Config.bytes(payload), 1, false);
        System.out.println("[sensor] published " + topic + " = " + payload);
    }
}
