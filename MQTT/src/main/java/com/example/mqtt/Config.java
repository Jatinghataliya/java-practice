package com.example.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;

public final class Config {

    private Config() {
    }

    public static String brokerUrl() {
        String url = System.getenv("MQTT_BROKER");
        return (url == null || url.isBlank()) ? "tcp://localhost:1883" : url;
    }

    public static MqttClient newClient(String clientId) throws MqttException {
        return new MqttClient(brokerUrl(), clientId, new MemoryPersistence());
    }

    /** Connects a clean-session client (the broker keeps no state for it after it disconnects). */
    public static MqttClient connect(String clientId) throws MqttException {
        MqttClient client = newClient(clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        client.connect(options);
        return client;
    }

    public static void close(MqttClient client) throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
        client.close();
    }

    public static String text(byte[] payload) {
        return new String(payload, StandardCharsets.UTF_8);
    }

    public static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
