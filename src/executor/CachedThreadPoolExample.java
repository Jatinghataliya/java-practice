package executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * CachedThreadPoolExample
 *
 * Demonstrates ExecutorService with a cached thread pool.
 * - Creates new threads as needed.
 * - Reuses idle threads that are available.
 * - Threads unused for 60 seconds are terminated and removed.
 * - Best for many short-lived, bursty tasks.
 *
 * ⚠️ WARNING: Avoid for long-running tasks — pool is unbounded and can
 *             spawn too many threads under heavy load.
 */
public class CachedThreadPoolExample {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Cached Thread Pool Example ===\n");

        ExecutorService executor = Executors.newCachedThreadPool();

        // Phase 1: Submit 10 quick tasks — pool creates threads on demand
        System.out.println("--- Phase 1: Burst of 10 quick tasks ---");
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Quick Task " + taskId
                        + " on: " + Thread.currentThread().getName());
                try {
                    Thread.sleep(100); // very short work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(500); // wait for phase 1 tasks to complete

        // Phase 2: Submit 5 more tasks — pool REUSES idle threads from phase 1
        System.out.println("\n--- Phase 2: Reusing idle threads ---");
        for (int i = 11; i <= 15; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Reused Task " + taskId
                        + " on: " + Thread.currentThread().getName());
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\nAll tasks completed.");
    }
}
