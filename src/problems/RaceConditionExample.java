package problems;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RaceConditionExample
 *
 * A Race Condition occurs when two or more threads access shared mutable data
 * simultaneously, and the final result depends on the unpredictable order of
 * thread scheduling — leading to INCORRECT results.
 *
 * Root cause: read-modify-write operations (e.g., count++) are NOT atomic.
 *   count++  is actually 3 steps:
 *     1. READ  current value of count
 *     2. ADD   1 to it
 *     3. WRITE the new value back
 *   Two threads can interleave these steps and BOTH write the same value.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Fixes:
 *   1. synchronized method / block  — mutual exclusion (one thread at a time)
 *   2. AtomicInteger                — lock-free CAS (Compare-And-Swap) operations
 *   3. ReentrantLock                — explicit locking
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class RaceConditionExample {

    private static final int THREADS      = 10;
    private static final int INCREMENTS   = 1000;
    private static final int EXPECTED     = THREADS * INCREMENTS; // 10_000

    // ── Version 1: BROKEN — unsynchronized counter ───────────────────────────
    static class UnsafeCounter {
        private int count = 0;

        public void increment() {
            count++; // NOT atomic: read → add → write (3 steps, interleave possible)
        }

        public int getCount() { return count; }
    }

    // ── Version 2: FIXED — synchronized counter ──────────────────────────────
    static class SynchronizedCounter {
        private int count = 0;

        public synchronized void increment() {
            count++; // only ONE thread can execute this at a time
        }

        public synchronized int getCount() { return count; }
    }

    // ── Version 3: FIXED — AtomicInteger (lock-free, faster) ─────────────────
    static class AtomicCounter {
        private final AtomicInteger count = new AtomicInteger(0);

        public void increment() {
            count.incrementAndGet(); // single atomic CAS operation — no lock needed
        }

        public int getCount() { return count.get(); }
    }

    // ── Helper: run N threads each doing M increments ────────────────────────
    static void runConcurrently(Runnable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    task.run();
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Race Condition Example ===");
        System.out.println("Threads: " + THREADS + " | Increments each: " + INCREMENTS);
        System.out.println("Expected final count: " + EXPECTED + "\n");

        // ── 1. Broken: UnsafeCounter ──────────────────────────────────────────
        System.out.println("--- 1. UnsafeCounter (no synchronization) ---");
        UnsafeCounter unsafe = new UnsafeCounter();
        runConcurrently(unsafe::increment);
        System.out.println("Actual count  : " + unsafe.getCount());
        System.out.println("Expected count: " + EXPECTED);
        boolean isWrong = unsafe.getCount() != EXPECTED;
        System.out.println("Race condition: " + (isWrong ? "⚠ YES — count is wrong!" : "OK (lucky this run)"));

        // ── 2. Fixed: SynchronizedCounter ────────────────────────────────────
        System.out.println("\n--- 2. SynchronizedCounter (synchronized method) ---");
        SynchronizedCounter synced = new SynchronizedCounter();
        runConcurrently(synced::increment);
        System.out.println("Actual count  : " + synced.getCount());
        System.out.println("Expected count: " + EXPECTED);
        System.out.println("Correct: " + (synced.getCount() == EXPECTED ? "✓ YES" : "✗ NO"));

        // ── 3. Fixed: AtomicCounter ───────────────────────────────────────────
        System.out.println("\n--- 3. AtomicCounter (lock-free AtomicInteger) ---");
        AtomicCounter atomic = new AtomicCounter();
        runConcurrently(atomic::increment);
        System.out.println("Actual count  : " + atomic.getCount());
        System.out.println("Expected count: " + EXPECTED);
        System.out.println("Correct: " + (atomic.getCount() == EXPECTED ? "✓ YES" : "✗ NO"));

        // ── Explanation ───────────────────────────────────────────────────────
        System.out.println("\n─────────────────────────────────────────────");
        System.out.println("WHY count++ is NOT thread-safe:");
        System.out.println("  Thread-1: READ  count = 5");
        System.out.println("  Thread-2: READ  count = 5   ← same value! (interleaved)");
        System.out.println("  Thread-1: ADD   5 + 1 = 6");
        System.out.println("  Thread-2: ADD   5 + 1 = 6");
        System.out.println("  Thread-1: WRITE count = 6");
        System.out.println("  Thread-2: WRITE count = 6   ← one increment LOST!");
        System.out.println("  Result: 6 instead of 7");
        System.out.println("─────────────────────────────────────────────");
    }
}
