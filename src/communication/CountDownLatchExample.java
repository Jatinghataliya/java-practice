package communication;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CountDownLatchExample
 *
 * CountDownLatch allows one or more threads to wait until a set of operations
 * in other threads complete.
 *
 * - Initialized with a count N.
 * - countDown() decrements the count by 1.
 * - await()     blocks the calling thread until count reaches 0.
 * - Once count reaches 0, it CANNOT be reset (use CyclicBarrier for reuse).
 *
 * Common use cases:
 *  - Wait for N services to start before proceeding.
 *  - Wait for N worker threads to finish before aggregating results.
 *  - "Starting gun" — hold N threads until a signal fires them all at once.
 */
public class CountDownLatchExample {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== CountDownLatch Example ===\n");

        // ── Example 1: Wait for all workers to finish ─────────────────────────
        System.out.println("--- Example 1: Wait for all workers to complete ---");

        int workerCount = 5;
        CountDownLatch latch = new CountDownLatch(workerCount);
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);

        for (int i = 1; i <= workerCount; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    System.out.println("[Worker-" + workerId + "] Working...");
                    Thread.sleep((long) (Math.random() * 1000)); // simulate work
                    System.out.println("[Worker-" + workerId + "] Done. countDown()");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown(); // always count down even if exception occurs
                }
            });
        }

        System.out.println("[Main] Waiting for all workers to finish...");
        latch.await(); // blocks until count = 0
        System.out.println("[Main] All workers done! Aggregating results.\n");

        executor.shutdown();

        // ── Example 2: Starting gun — release all threads simultaneously ──────
        System.out.println("--- Example 2: Starting gun (release threads together) ---");

        CountDownLatch startGun = new CountDownLatch(1); // count = 1 acts as a gate
        CountDownLatch finishLine = new CountDownLatch(3);

        for (int i = 1; i <= 3; i++) {
            final int runnerId = i;
            new Thread(() -> {
                try {
                    System.out.println("[Runner-" + runnerId + "] Ready at start line...");
                    startGun.await(); // all runners wait for the gun
                    System.out.println("[Runner-" + runnerId + "] GO! Running...");
                    Thread.sleep((long) (Math.random() * 800));
                    System.out.println("[Runner-" + runnerId + "] Finished!");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            }, "Runner-" + runnerId).start();
        }

        Thread.sleep(500); // let all runners reach the start line
        System.out.println("[Starter] Ready... Set... GO!");
        startGun.countDown(); // fires all 3 runners at the same time

        finishLine.await(); // wait for all to finish
        System.out.println("[Starter] All runners finished the race!");
    }
}
