package com.example.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Retained message: the broker stores the last retained value per topic and gives it to every new subscriber. */
public final class RetainedDemo {

    private static final String TOPIC = "devices/thermostat/state";

    private RetainedDemo() {
    }

    public static void run() throws Exception {
        MqttClient device = Config.connect("thermostat");
        // retained = true; the subscriber does not exist yet
        device.publish(TOPIC, Config.bytes("heating"), 1, true);
        System.out.println("[thermostat] published retained state = heating");

        CountDownLatch done = new CountDownLatch(1);
        MqttClient lateSubscriber = Config.connect("late-subscriber");
        lateSubscriber.subscribe(TOPIC, 1, (topic, message) -> {
            System.out.printf("[late-subscriber] got %s = %s (retained flag: %s)%n",
                    topic, Config.text(message.getPayload()), message.isRetained());
            done.countDown();
        });

        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Retained message was not delivered");
        }

        Config.close(lateSubscriber);

        // An empty retained payload clears the retained value from the broker
        device.publish(TOPIC, new byte[0], 1, true);
        System.out.println("[thermostat] cleared retained state");

        Config.close(device);
    }
}
