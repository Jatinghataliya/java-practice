package problems;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DeadlockExample
 *
 * A Deadlock occurs when two or more threads are PERMANENTLY BLOCKED,
 * each waiting for a lock held by the other — neither can ever proceed.
 *
 * Classic scenario:
 *   Thread-1 holds Lock-A, waits for Lock-B
 *   Thread-2 holds Lock-B, waits for Lock-A  ← circular wait = DEADLOCK
 *
 * Four conditions for deadlock (Coffman conditions — ALL must hold):
 *   1. Mutual Exclusion  — resources cannot be shared
 *   2. Hold and Wait     — thread holds one lock while waiting for another
 *   3. No Preemption     — locks cannot be forcibly taken
 *   4. Circular Wait     — circular chain of threads waiting on each other
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Prevention strategies (demonstrated below):
 *   1. Lock ordering    — always acquire locks in the SAME fixed order
 *   2. tryLock()        — attempt lock with timeout; back off if not acquired
 *   3. Single lock      — use one lock for both resources (if possible)
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class DeadlockExample {

    // Two shared resources
    static final Object LOCK_A = new Object();
    static final Object LOCK_B = new Object();

    // ── 1. DEADLOCK — Thread-1 and Thread-2 acquire locks in OPPOSITE order ──
    static void demonstrateDeadlock() throws InterruptedException {
        System.out.println("--- Deadlock Demo (will hang for 3 seconds then be interrupted) ---\n");

        Thread thread1 = new Thread(() -> {
            synchronized (LOCK_A) {
                System.out.println("[Thread-1] Acquired Lock-A. Waiting for Lock-B...");
                sleep(100); // give Thread-2 time to acquire Lock-B
                synchronized (LOCK_B) {
                    // Thread-1 will NEVER reach here — Thread-2 holds Lock-B
                    System.out.println("[Thread-1] Acquired Lock-B. (This won't print in deadlock)");
                }
            }
        }, "Thread-1");

        Thread thread2 = new Thread(() -> {
            synchronized (LOCK_B) {
                System.out.println("[Thread-2] Acquired Lock-B. Waiting for Lock-A...");
                sleep(100);
                synchronized (LOCK_A) {
                    // Thread-2 will NEVER reach here — Thread-1 holds Lock-A
                    System.out.println("[Thread-2] Acquired Lock-A. (This won't print in deadlock)");
                }
            }
        }, "Thread-2");

        thread1.start();
        thread2.start();

        // Let threads deadlock for 3 seconds, then interrupt them
        Thread.sleep(3000);
        thread1.interrupt();
        thread2.interrupt();
        thread1.join(500);
        thread2.join(500);

        System.out.println("[Main] Both threads interrupted. Deadlock broken externally.\n");
        System.out.println("  Thread-1 state: " + thread1.getState());
        System.out.println("  Thread-2 state: " + thread2.getState());
    }

    // ── 2. FIX 1: Lock Ordering — always acquire locks in the SAME order ─────
    static void fixWithLockOrdering() throws InterruptedException {
        System.out.println("\n--- Fix 1: Lock Ordering (always A before B) ---\n");

        Thread thread1 = new Thread(() -> {
            synchronized (LOCK_A) {              // Thread-1: A first
                System.out.println("[Thread-1] Acquired Lock-A");
                sleep(50);
                synchronized (LOCK_B) {          // then B
                    System.out.println("[Thread-1] Acquired Lock-B — task done!");
                }
            }
        }, "Thread-1");

        Thread thread2 = new Thread(() -> {
            synchronized (LOCK_A) {              // Thread-2: also A first (same order!)
                System.out.println("[Thread-2] Acquired Lock-A");
                sleep(50);
                synchronized (LOCK_B) {          // then B
                    System.out.println("[Thread-2] Acquired Lock-B — task done!");
                }
            }
        }, "Thread-2");

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        System.out.println("[Main] No deadlock — both completed.\n");
    }

    // ── 3. FIX 2: tryLock with timeout — back off if lock not acquired ────────
    static void fixWithTryLock() throws InterruptedException {
        System.out.println("--- Fix 2: tryLock() with timeout ---\n");

        ReentrantLock lockA = new ReentrantLock();
        ReentrantLock lockB = new ReentrantLock();

        Runnable task1 = () -> {
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    if (lockA.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            sleep(50);
                            if (lockB.tryLock(100, TimeUnit.MILLISECONDS)) {
                                try {
                                    System.out.println("[Thread-1] Got both locks. Done!");
                                    return; // success
                                } finally {
                                    lockB.unlock();
                                }
                            } else {
                                System.out.println("[Thread-1] Attempt " + attempt
                                        + " — couldn't get Lock-B. Backing off...");
                            }
                        } finally {
                            lockA.unlock(); // always release Lock-A
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                sleep(50 * attempt); // exponential back-off
            }
        };

        Runnable task2 = () -> {
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    if (lockB.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            sleep(50);
                            if (lockA.tryLock(100, TimeUnit.MILLISECONDS)) {
                                try {
                                    System.out.println("[Thread-2] Got both locks. Done!");
                                    return; // success
                                } finally {
                                    lockA.unlock();
                                }
                            } else {
                                System.out.println("[Thread-2] Attempt " + attempt
                                        + " — couldn't get Lock-A. Backing off...");
                            }
                        } finally {
                            lockB.unlock(); // always release Lock-B
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                sleep(50 * attempt);
            }
        };

        Thread t1 = new Thread(task1, "Thread-1");
        Thread t2 = new Thread(task2, "Thread-2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("[Main] No deadlock — tryLock backed off and retried.\n");
    }

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws InterruptedException {

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║        DEADLOCK Example              ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        System.out.println("Condition for Deadlock:");
        System.out.println("  Thread-1: holds Lock-A → waits for Lock-B");
        System.out.println("  Thread-2: holds Lock-B → waits for Lock-A");
        System.out.println("  → Circular wait = DEADLOCK\n");

        demonstrateDeadlock();
        fixWithLockOrdering();
        fixWithTryLock();

        System.out.println("══════════════════════════════════════════");
        System.out.println("Summary of Prevention Strategies:");
        System.out.println("  1. Lock Ordering  — always acquire locks in the same global order");
        System.out.println("  2. tryLock()      — attempt with timeout, back off if unavailable");
        System.out.println("  3. Single Lock    — use one lock if both resources always used together");
        System.out.println("  4. Lock Timeout   — set max wait time to avoid infinite blocking");
        System.out.println("══════════════════════════════════════════");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
