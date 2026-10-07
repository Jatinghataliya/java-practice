package communication;

/**
 * NotifyAllExample
 *
 * Demonstrates notifyAll() — wakes up ALL threads waiting on the same lock.
 *
 * notify()    → wakes ONE random waiting thread (unpredictable which one).
 * notifyAll() → wakes ALL waiting threads; they compete for the lock one by one.
 *
 * Use notifyAll() when multiple threads may be waiting for the same condition,
 * to avoid a situation where the wrong thread is notified and others starve.
 */
public class NotifyAllExample {

    private static final Object lock = new Object();
    private static boolean taskComplete = false;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== notifyAll() Example ===\n");

        // Create 5 worker threads — all wait for task to complete
        Thread[] workers = new Thread[5];
        for (int i = 1; i <= 5; i++) {
            final int workerId = i;
            workers[i - 1] = new Thread(() -> {
                synchronized (lock) {
                    System.out.println("[Worker-" + workerId + "] Waiting for task...");
                    while (!taskComplete) {
                        try {
                            lock.wait(); // all 5 threads wait here
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    // All workers execute this once woken up
                    System.out.println("[Worker-" + workerId + "] Task complete! Starting work.");
                }
            }, "Worker-" + workerId);

            workers[i - 1].start();
        }

        Thread.sleep(1000); // let all workers enter wait()

        // Coordinator thread — completes task and notifies ALL workers
        Thread coordinator = new Thread(() -> {
            synchronized (lock) {
                System.out.println("\n[Coordinator] Task finished! Notifying ALL workers...");
                taskComplete = true;
                lock.notifyAll(); // wake up ALL 5 waiting threads at once
                System.out.println("[Coordinator] notifyAll() called.\n");
            }
        }, "Coordinator");

        coordinator.start();

        // Wait for everyone to finish
        for (Thread w : workers) w.join();
        coordinator.join();

        // ── Contrast: notify() vs notifyAll() ──────────────────────────────
        System.out.println("\n--- Contrast: notify() would only wake ONE worker ---");
        System.out.println("    notify()    → 1 of 5 workers woken (others starve)");
        System.out.println("    notifyAll() → all 5 workers woken (safe)");
    }
}
