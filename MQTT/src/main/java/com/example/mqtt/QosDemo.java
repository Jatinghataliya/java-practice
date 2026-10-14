package com.example.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Quality of Service: 0 = at most once, 1 = at least once, 2 = exactly once.
 * The delivered QoS is the lower of the publish QoS and the subscription QoS.
 */
public final class QosDemo {

    private QosDemo() {
    }

    public static void run() throws Exception {
        // 3 messages x 2 subscribers
        CountDownLatch done = new CountDownLatch(6);

        MqttClient high = Config.connect("qos-high-subscriber");
        high.subscribe("qos/test", 2, (topic, message) -> {
            System.out.printf("[subscribed at QoS 2] got \"%s\" delivered with QoS %d%n",
                    Config.text(message.getPayload()), message.getQos());
            done.countDown();
        });

        MqttClient low = Config.connect("qos-low-subscriber");
        low.subscribe("qos/test", 0, (topic, message) -> {
            System.out.printf("[subscribed at QoS 0] got \"%s\" delivered with QoS %d%n",
                    Config.text(message.getPayload()), message.getQos());
            done.countDown();
        });

        MqttClient publisher = Config.connect("qos-publisher");
        for (int qos = 0; qos <= 2; qos++) {
            publisher.publish("qos/test", Config.bytes("published at QoS " + qos), qos, false);
            System.out.println("[publisher] sent at QoS " + qos);
        }

        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Not all QoS messages were received");
        }
        Config.close(publisher);
        Config.close(high);
        Config.close(low);
    }
}
