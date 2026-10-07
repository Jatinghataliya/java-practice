package problems;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * StarvationExample
 *
 * Starvation occurs when a thread is PERPETUALLY DENIED access to a resource
 * because other threads (usually higher-priority ones) continuously consume it,
 * leaving the starved thread waiting indefinitely — but NOT deadlocked.
 *
 * Unlike deadlock:
 *   - Deadlock   → ALL threads are blocked (circular wait)
 *   - Starvation → SOME threads run fine; ONE thread never gets a turn
 *
 * Common causes:
 *   1. Thread priority — low-priority threads starved by high-priority ones
 *   2. Unfair locks    — a non-fair lock keeps re-granting to the same threads
 *   3. Long critical sections — one thread holds the lock for too long
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Fix:
 *   Use a FAIR ReentrantLock(true) — grants the lock to the longest-waiting thread
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class StarvationExample {

    private static final int RUN_SECONDS = 4;

    // ── Counters to track how often each thread gets the lock ─────────────────
    static int[] accessCount = new int[5];

    // ── 1. STARVATION — unfair synchronized / non-fair lock ──────────────────
    static void demonstrateStarvation() throws InterruptedException {
        System.out.println("--- Starvation Demo (unfair lock, 4 seconds) ---\n");

        accessCount = new int[5];
        final Object unfairLock = new Object();
        final long deadline = System.currentTimeMillis() + (RUN_SECONDS * 1000L);

        // Thread-0 is "greedy" — holds lock for a longer time
        // Threads 1-4 get very little time
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int id = i;
            final long holdTime = (id == 0) ? 80 : 5; // Thread-0 hogs the lock
            threads[i] = new Thread(() -> {
                while (System.currentTimeMillis() < deadline) {
                    synchronized (unfairLock) {
                        accessCount[id]++;
                        sleep(holdTime); // Thread-0 holds much longer
                    }
                    sleep(2); // brief pause before trying again
                }
            }, "Thread-" + id);
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("Lock access counts (unfair — Thread-0 is greedy):");
        for (int i = 0; i < 5; i++) {
            String bar = "█".repeat(Math.min(accessCount[i], 80));
            System.out.printf("  Thread-%d: %4d  %s%n", i, accessCount[i], bar);
        }
        System.out.println();
    }

    // ── 2. FIX: Fair ReentrantLock — longest-waiting thread gets the lock ────
    static void fixWithFairLock() throws InterruptedException {
        System.out.println("--- Fix: Fair ReentrantLock(true) (4 seconds) ---\n");

        accessCount = new int[5];
        // fair=true → lock is granted in FIFO order — threads wait in queue
        ReentrantLock fairLock = new ReentrantLock(true);
        final long deadline = System.currentTimeMillis() + (RUN_SECONDS * 1000L);

        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int id = i;
            final long holdTime = (id == 0) ? 80 : 5; // Thread-0 still tries to hog
            threads[i] = new Thread(() -> {
                while (System.currentTimeMillis() < deadline) {
                    fairLock.lock();
                    try {
                        accessCount[id]++;
                        sleep(holdTime);
                    } finally {
                        fairLock.unlock();
                    }
                    sleep(2);
                }
            }, "Thread-" + id);
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("Lock access counts (fair — threads take turns):");
        for (int i = 0; i < 5; i++) {
            String bar = "█".repeat(Math.min(accessCount[i], 80));
            System.out.printf("  Thread-%d: %4d  %s%n", i, accessCount[i], bar);
        }
        System.out.println();
    }

    // ── 3. Starvation via thread priority ────────────────────────────────────
    static void demonstratePriorityStarvation() throws InterruptedException {
        System.out.println("--- Priority Starvation Demo (2 seconds) ---\n");

        int[] priorityCount = new int[3];
        final long deadline = System.currentTimeMillis() + 2000L;

        Thread low    = new Thread(() -> { while (System.currentTimeMillis() < deadline) { priorityCount[0]++; } }, "Low-Priority");
        Thread normal = new Thread(() -> { while (System.currentTimeMillis() < deadline) { priorityCount[1]++; } }, "Normal-Priority");
        Thread high   = new Thread(() -> { while (System.currentTimeMillis() < deadline) { priorityCount[2]++; } }, "High-Priority");

        low.setPriority(Thread.MIN_PRIORITY);     // 1
        normal.setPriority(Thread.NORM_PRIORITY); // 5
        high.setPriority(Thread.MAX_PRIORITY);    // 10

        low.start(); normal.start(); high.start();
        low.join();  normal.join();  high.join();

        System.out.println("Iterations completed (higher priority = more CPU time):");
        System.out.printf("  Low    (priority 1):  %,d%n", priorityCount[0]);
        System.out.printf("  Normal (priority 5):  %,d%n", priorityCount[1]);
        System.out.printf("  High   (priority 10): %,d%n", priorityCount[2]);
        System.out.println("  (Note: priority is a hint — OS scheduler has final say)\n");
    }

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws InterruptedException {

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║        STARVATION Example            ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        System.out.println("Starvation: a thread never gets CPU/lock time because");
        System.out.println("others continuously take it — thread is alive but never progresses.\n");

        demonstrateStarvation();
        fixWithFairLock();
        demonstratePriorityStarvation();

        System.out.println("══════════════════════════════════════════");
        System.out.println("Summary:");
        System.out.println("  Problem : Non-fair lock favours threads that re-acquire quickly");
        System.out.println("  Fix 1   : ReentrantLock(true)  — FIFO fair ordering");
        System.out.println("  Fix 2   : Avoid very long critical sections");
        System.out.println("  Fix 3   : Use equal thread priorities for cooperative tasks");
        System.out.println("══════════════════════════════════════════");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
