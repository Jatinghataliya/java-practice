package executor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * BasicExecutorExample
 *
 * Demonstrates the simplest use of the Executor interface.
 * Executor decouples task submission from thread management.
 * It has only one method: execute(Runnable command)
 */
public class BasicExecutorExample {

    public static void main(String[] args) {

        System.out.println("=== Basic Executor Example ===\n");

        // 1. Executor backed by a single thread
        Executor executor = Executors.newSingleThreadExecutor();

        // Submit 5 tasks — Executor decides how/when to run them
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println("Task " + taskId
                        + " running on: " + Thread.currentThread().getName());
            });
        }

        // 2. Executor backed by a fixed thread pool
        Executor poolExecutor = Executors.newFixedThreadPool(3);

        System.out.println("\n--- Fixed Pool Executor ---");
        for (int i = 1; i <= 6; i++) {
            final int taskId = i;
            poolExecutor.execute(() -> {
                System.out.println("Pool Task " + taskId
                        + " running on: " + Thread.currentThread().getName());
            });
        }

        // Give threads time to finish before main exits
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\nMain thread: " + Thread.currentThread().getName());
    }
}
