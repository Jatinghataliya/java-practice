package executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SingleThreadExecutorExample
 *
 * Demonstrates ExecutorService with a single-thread executor.
 * - Only ONE thread executes tasks.
 * - Tasks are guaranteed to execute SEQUENTIALLY in submission order.
 * - Best when tasks must not run concurrently (e.g., ordered logging, DB writes).
 */
public class SingleThreadExecutorExample {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Single Thread Executor Example ===\n");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        // All tasks run one after another on the same single thread
        String[] steps = {
            "Step 1: Connect to database",
            "Step 2: Fetch records",
            "Step 3: Process records",
            "Step 4: Write results",
            "Step 5: Close connection"
        };

        for (String step : steps) {
            executor.submit(() -> {
                System.out.println("[" + Thread.currentThread().getName() + "] " + step);
                try {
                    Thread.sleep(300); // simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\nAll steps completed in order.");
    }
}
