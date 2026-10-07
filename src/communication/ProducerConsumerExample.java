package communication;

import java.util.LinkedList;
import java.util.Queue;

/**
 * ProducerConsumerExample
 *
 * Classic Producer-Consumer pattern using wait() and notify().
 *
 * - Producer adds items to a shared buffer.
 * - Consumer removes items from the shared buffer.
 * - When buffer is FULL  → Producer waits.
 * - When buffer is EMPTY → Consumer waits.
 * - After each produce/consume → notify() wakes the other thread.
 */
public class ProducerConsumerExample {

    private static final Queue<Integer> buffer = new LinkedList<>();
    private static final int CAPACITY = 3;
    private static final Object lock = new Object();

    // ── Producer ──────────────────────────────────────────────────────────────
    static class Producer implements Runnable {
        @Override
        public void run() {
            int value = 1;
            while (value <= 10) {
                synchronized (lock) {
                    // Wait while buffer is full
                    while (buffer.size() == CAPACITY) {
                        try {
                            System.out.println("[Producer] Buffer full. Waiting...");
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    buffer.offer(value);
                    System.out.println("[Producer] Produced: " + value
                            + "  | Buffer: " + buffer);
                    value++;
                    lock.notify(); // wake up consumer
                }

                try { Thread.sleep(200); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // ── Consumer ──────────────────────────────────────────────────────────────
    static class Consumer implements Runnable {
        @Override
        public void run() {
            int consumed = 0;
            while (consumed < 10) {
                synchronized (lock) {
                    // Wait while buffer is empty
                    while (buffer.isEmpty()) {
                        try {
                            System.out.println("[Consumer] Buffer empty. Waiting...");
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    int value = buffer.poll();
                    System.out.println("[Consumer] Consumed: " + value
                            + "  | Buffer: " + buffer);
                    consumed++;
                    lock.notify(); // wake up producer
                }

                try { Thread.sleep(400); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Producer-Consumer Example ===\n");
        System.out.println("Buffer capacity: " + CAPACITY);
        System.out.println("Producer speed: 200ms | Consumer speed: 400ms\n");

        Thread producer = new Thread(new Producer(), "Producer");
        Thread consumer = new Thread(new Consumer(), "Consumer");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();

        System.out.println("\nAll items produced and consumed.");
    }
}
