package executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * GracefulShutdownExample
 *
 * Demonstrates the correct pattern to shut down an ExecutorService.
 *
 * Shutdown states:
 *   Running → shutdown() called → Shutdown (no new tasks, finishes queued)
 *           → awaitTermination() timeout → shutdownNow() → Terminated
 *
 * shutdown()             : Stops accepting new tasks; queued tasks still run.
 * shutdownNow()          : Attempts to stop all running tasks; returns pending ones.
 * awaitTermination()     : Blocks until all tasks finish or timeout expires.
 * isShutdown()           : true after shutdown() is called.
 * isTerminated()         : true after all tasks finish post-shutdown.
 */
public class GracefulShutdownExample {

    public static void main(String[] args) {

        System.out.println("=== Graceful Shutdown Example ===\n");

        // --- Example 1: Clean graceful shutdown ---
        System.out.println("--- Example 1: Graceful shutdown ---");
        ExecutorService executor1 = Executors.newFixedThreadPool(3);

        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor1.submit(() -> {
                System.out.println("Task " + taskId + " running...");
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Task " + taskId + " done.");
            });
        }

        gracefulShutdown(executor1, 5);

        // --- Example 2: Forced shutdown when tasks take too long ---
        System.out.println("\n--- Example 2: Forced shutdown (tasks too slow) ---");
        ExecutorService executor2 = Executors.newFixedThreadPool(2);

        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            executor2.submit(() -> {
                System.out.println("Long Task " + taskId + " started...");
                try {
                    Thread.sleep(5000); // takes 5 seconds — too long
                } catch (InterruptedException e) {
                    System.out.println("Long Task " + taskId + " interrupted!");
                    Thread.currentThread().interrupt();
                }
            });
        }

        gracefulShutdown(executor2, 1); // only wait 1 second → forces shutdownNow()

        System.out.println("\nBoth executors terminated.");
    }

    /**
     * Reusable graceful shutdown utility.
     * Waits up to [timeoutSeconds] for tasks to finish, then forces stop.
     */
    private static void gracefulShutdown(ExecutorService executor, int timeoutSeconds) {
        executor.shutdown(); // no new tasks accepted
        System.out.println("isShutdown: " + executor.isShutdown());

        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                System.out.println("Timeout reached! Forcing shutdown...");
                executor.shutdownNow(); // interrupt running tasks

                // Wait a bit more for interrupted tasks to respond
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    System.out.println("Executor did not terminate cleanly.");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("isTerminated: " + executor.isTerminated());
    }
}
