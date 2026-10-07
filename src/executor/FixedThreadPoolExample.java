package executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * FixedThreadPoolExample
 *
 * Demonstrates ExecutorService with a fixed thread pool.
 * - Pool always maintains exactly N threads.
 * - Tasks beyond N are queued and executed as threads become free.
 * - Best for stable, predictable workloads.
 */
public class FixedThreadPoolExample {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Fixed Thread Pool Example ===\n");

        // Pool of 3 threads — only 3 tasks run at a time
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Submit 9 tasks — 3 run immediately, rest wait in queue
        for (int i = 1; i <= 9; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId
                        + " started   on: " + Thread.currentThread().getName());
                try {
                    // Simulate some work
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Task " + taskId
                        + " finished  on: " + Thread.currentThread().getName());
            });
        }

        System.out.println("All 9 tasks submitted. Pool has 3 threads.\n");

        // Graceful shutdown
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\nAll tasks completed. Executor shut down.");
    }
}
