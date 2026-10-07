package communication;

/**
 * VolatileExample
 *
 * Demonstrates the volatile keyword for inter-thread visibility.
 *
 * Without volatile:
 *   - Each thread may cache a variable in its own CPU register/cache.
 *   - Changes made by one thread may NOT be visible to another thread.
 *
 * With volatile:
 *   - Every read/write goes directly to main memory.
 *   - Guarantees VISIBILITY across threads.
 *   - Does NOT guarantee atomicity (use AtomicInteger for that).
 *
 * Typical use: simple boolean flags shared between threads.
 */
public class VolatileExample {

    // ── Without volatile (broken — may loop forever) ─────────────────────────
    static boolean stopWithoutVolatile = false;  // NOT volatile — unsafe!

    // ── With volatile (correct) ───────────────────────────────────────────────
    static volatile boolean stopWithVolatile = false;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== volatile Keyword Example ===\n");

        // ── Example 1: volatile flag stops the loop reliably ─────────────────
        System.out.println("--- Example 1: Stopping a thread with volatile flag ---");

        Thread worker = new Thread(() -> {
            System.out.println("[Worker] Started. Running until stopWithVolatile = true...");
            int count = 0;
            while (!stopWithVolatile) {  // reads fresh value from main memory every time
                count++;
            }
            System.out.println("[Worker] Stopped! Counted: " + count);
        }, "Worker");

        worker.start();
        Thread.sleep(500); // let worker run a bit

        System.out.println("[Main]   Setting stopWithVolatile = true...");
        stopWithVolatile = true; // visible to worker immediately due to volatile
        worker.join();
        System.out.println("[Main]   Worker stopped cleanly.\n");

        // ── Example 2: volatile visibility with data flag ─────────────────────
        System.out.println("--- Example 2: volatile for data handoff ---");

        final int[] data = {0};
        volatile boolean[] dataReady = {false};  // simulate volatile with array trick

        // Use a proper volatile field instead
        VolatileFlag flag = new VolatileFlag();

        Thread producer = new Thread(() -> {
            data[0] = 42;                          // write data first
            flag.ready = true;                     // volatile write — acts as memory fence
            System.out.println("[Producer] Data written: " + data[0] + ", ready = true");
        });

        Thread consumer = new Thread(() -> {
            while (!flag.ready) { /* spin */ }     // volatile read
            System.out.println("[Consumer] Saw ready = true. Data = " + data[0]);
        });

        consumer.start();
        Thread.sleep(100);
        producer.start();

        producer.join();
        consumer.join();

        // ── Key reminder ──────────────────────────────────────────────────────
        System.out.println("\n--- volatile vs synchronized vs AtomicInteger ---");
        System.out.println("volatile        → visibility only (no atomicity)");
        System.out.println("synchronized    → visibility + atomicity + mutual exclusion");
        System.out.println("AtomicInteger   → lock-free atomic operations (count++, etc.)");
    }

    // Helper class to hold a volatile field
    static class VolatileFlag {
        volatile boolean ready = false;
    }
}
