package communication;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * BlockingQueueExample
 *
 * BlockingQueue is a thread-safe queue that:
 *  - Blocks the PRODUCER when the queue is full  (put() waits).
 *  - Blocks the CONSUMER when the queue is empty (take() waits).
 *
 * No explicit wait()/notify() needed — BlockingQueue handles it internally.
 *
 * Common implementations:
 *  - ArrayBlockingQueue  — bounded, backed by array, FIFO.
 *  - LinkedBlockingQueue — optionally bounded, backed by linked nodes.
 *  - PriorityBlockingQueue — unbounded, orders by priority.
 *  - SynchronousQueue    — no internal capacity; each put must wait for a take.
 *
 * Key methods:
 *  put(e)          — inserts, blocks if full.
 *  take()          — removes, blocks if empty.
 *  offer(e)        — inserts, returns false if full (non-blocking).
 *  poll()          — removes, returns null if empty (non-blocking).
 *  offer(e, t, u)  — inserts with timeout.
 *  poll(t, u)      — removes with timeout.
 */
public class BlockingQueueExample {

    private static final int CAPACITY = 5;
    private static final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(CAPACITY);
    private static volatile boolean producingDone = false;

    // ── Producer ──────────────────────────────────────────────────────────────
    static class Producer implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 1; i <= 10; i++) {
                    queue.put(i); // blocks if queue is full
                    System.out.println("[Producer] Produced: " + i
                            + "  | Queue size: " + queue.size());
                    Thread.sleep(150);
                }
                producingDone = true;
                System.out.println("[Producer] All items produced.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ── Consumer ──────────────────────────────────────────────────────────────
    static class Consumer implements Runnable {
        private final String name;

        Consumer(String name) { this.name = name; }

        @Override
        public void run() {
            try {
                while (!producingDone || !queue.isEmpty()) {
                    // poll with timeout — avoids infinite block when producer is done
                    Integer value = queue.poll(500, TimeUnit.MILLISECONDS);
                    if (value != null) {
                        System.out.println("[" + name + "] Consumed: " + value
                                + "  | Queue size: " + queue.size());
                        Thread.sleep(300);
                    }
                }
                System.out.println("[" + name + "] No more items. Exiting.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== BlockingQueue Producer-Consumer Example ===\n");
        System.out.println("Queue capacity: " + CAPACITY);
        System.out.println("1 Producer (150ms) | 2 Consumers (300ms each)\n");

        Thread producer   = new Thread(new Producer(),          "Producer");
        Thread consumer1  = new Thread(new Consumer("Consumer-1"), "Consumer-1");
        Thread consumer2  = new Thread(new Consumer("Consumer-2"), "Consumer-2");

        producer.start();
        consumer1.start();
        consumer2.start();

        producer.join();
        consumer1.join();
        consumer2.join();

        System.out.println("\nAll items processed. Queue is empty: " + queue.isEmpty());

        // ── Bonus: LinkedBlockingQueue (unbounded) ────────────────────────────
        System.out.println("\n--- Bonus: LinkedBlockingQueue (unbounded) ---");
        BlockingQueue<String> linkedQueue = new LinkedBlockingQueue<>();

        linkedQueue.offer("Message-1");
        linkedQueue.offer("Message-2");
        linkedQueue.offer("Message-3");

        System.out.println("Queue: " + linkedQueue);
        System.out.println("Taken: " + linkedQueue.take());
        System.out.println("Queue after take: " + linkedQueue);
    }
}
