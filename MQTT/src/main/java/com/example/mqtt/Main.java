package com.example.mqtt;

public final class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("Using broker: " + Config.brokerUrl());

        section("1. Publish/Subscribe with topic wildcards");
        PubSubDemo.run();

        section("2. Quality of Service (0, 1, 2)");
        QosDemo.run();

        section("3. Retained message");
        RetainedDemo.run();

        section("4. Last Will and Testament");
        LastWillDemo.run();

        section("5. Persistent session (offline delivery)");
        PersistentSessionDemo.run();

        System.out.println("\nAll demos completed.");
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
