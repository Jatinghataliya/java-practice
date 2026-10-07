package communication;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CyclicBarrierExample
 *
 * CyclicBarrier makes a set of threads wait for each other at a common barrier point.
 * Once ALL threads reach the barrier, they are ALL released simultaneously.
 *
 * Key difference from CountDownLatch:
 *  - CountDownLatch: one-time use, threads wait for other threads to finish tasks.
 *  - CyclicBarrier:  reusable, threads wait for each other at a sync point (phase-by-phase).
 *
 * Optional barrier action: a Runnable that runs once when all threads arrive.
 *
 * Use cases:
 *  - Multi-phase parallel computation (each phase must finish before next starts).
 *  - Parallel simulations where all workers sync between rounds.
 */
public class CyclicBarrierExample {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== CyclicBarrier Example ===\n");

        int threadCount = 3;

        // Barrier action runs once all 3 threads reach the barrier
        CyclicBarrier barrier = new CyclicBarrier(threadCount, () ->
                System.out.println("\n>>> All threads reached barrier! Starting next phase...\n")
        );

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // ── 3 phases of parallel work ─────────────────────────────────────────
        for (int t = 1; t <= threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // ── Phase 1 ──
                    System.out.println("[Thread-" + threadId + "] Phase 1 working...");
                    Thread.sleep((long) (Math.random() * 800 + 200));
                    System.out.println("[Thread-" + threadId + "] Phase 1 done. Waiting at barrier...");
                    barrier.await(); // wait for all 3 threads

                    // ── Phase 2 ──
                    System.out.println("[Thread-" + threadId + "] Phase 2 working...");
                    Thread.sleep((long) (Math.random() * 800 + 200));
                    System.out.println("[Thread-" + threadId + "] Phase 2 done. Waiting at barrier...");
                    barrier.await(); // barrier resets automatically — reusable!

                    // ── Phase 3 ──
                    System.out.println("[Thread-" + threadId + "] Phase 3 working...");
                    Thread.sleep((long) (Math.random() * 500 + 100));
                    System.out.println("[Thread-" + threadId + "] Phase 3 done. Waiting at barrier...");
                    barrier.await();

                    System.out.println("[Thread-" + threadId + "] All phases complete!");

                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(100);
        }

        System.out.println("\nAll threads finished all phases.");
        System.out.println("Barrier was reused " + threadCount + " times (cyclic!)");
    }
}
