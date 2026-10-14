package com.example.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Last Will and Testament: the client registers a message at connect time. If it disappears
 * without sending DISCONNECT (crash, network loss), the broker publishes the will for it.
 */
public final class LastWillDemo {

    private static final String STATUS_TOPIC = "devices/sensor42/status";

    private LastWillDemo() {
    }

    public static void run() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        MqttClient monitor = Config.connect("monitor");
        monitor.subscribe(STATUS_TOPIC, 1, (topic, message) -> {
            System.out.printf("[monitor] %s = %s%n", topic, Config.text(message.getPayload()));
            done.countDown();
        });

        MqttClient device = Config.newClient("sensor42");
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setWill(STATUS_TOPIC, Config.bytes("OFFLINE (unexpected)"), 1, false);
        device.connect(options);
        System.out.println("[sensor42] connected with a last will");

        // Drop the TCP connection without a DISCONNECT packet, simulating a crash
        device.disconnectForcibly(0, 0, false);
        System.out.println("[sensor42] connection lost abruptly");

        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Last will was not delivered");
        }
        device.close();
        Config.close(monitor);
    }
}
