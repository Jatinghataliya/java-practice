package executor;

import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * ScheduledExecutorExample
 *
 * Demonstrates ScheduledExecutorService for:
 * 1. schedule()                — run once after a delay
 * 2. scheduleAtFixedRate()     — run repeatedly at a fixed interval from start
 * 3. scheduleWithFixedDelay()  — run repeatedly with fixed delay after last finish
 */
public class ScheduledExecutorExample {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== ScheduledExecutorService Example ===\n");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

        // --- Example 1: schedule() — run once after a delay ---
        System.out.println("--- schedule(): Run once after 2 seconds ---");
        scheduler.schedule(() -> {
            System.out.println("[" + LocalTime.now() + "] One-time task executed!");
        }, 2, TimeUnit.SECONDS);

        // --- Example 2: scheduleAtFixedRate() ---
        // First run: after 1 second, then every 2 seconds regardless of task duration
        System.out.println("--- scheduleAtFixedRate(): Every 2 seconds ---");
        ScheduledFuture<?> fixedRateFuture = scheduler.scheduleAtFixedRate(() -> {
            System.out.println("[" + LocalTime.now() + "] Fixed-rate tick");
        }, 1, 2, TimeUnit.SECONDS);

        // --- Example 3: scheduleWithFixedDelay() ---
        // First run: after 1 second, then 2 seconds AFTER each task completes
        System.out.println("--- scheduleWithFixedDelay(): 2s delay after each run ---");
        ScheduledFuture<?> fixedDelayFuture = scheduler.scheduleWithFixedDelay(() -> {
            System.out.println("[" + LocalTime.now() + "] Fixed-delay tick (takes 500ms)");
            try {
                Thread.sleep(500); // simulate work time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 1, 2, TimeUnit.SECONDS);

        // Let examples run for 7 seconds
        Thread.sleep(7000);

        // Cancel the repeating tasks
        fixedRateFuture.cancel(false);
        fixedDelayFuture.cancel(false);

        System.out.println("\nRepeating tasks cancelled.");

        scheduler.shutdown();
        scheduler.awaitTermination(3, TimeUnit.SECONDS);
        System.out.println("Scheduler shut down.");
    }
}
