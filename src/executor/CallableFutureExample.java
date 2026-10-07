package executor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CallableFutureExample
 *
 * Demonstrates Callable and Future with ExecutorService.
 * - Callable<T> is like Runnable but returns a value and can throw exceptions.
 * - Future<T> represents the result of an async computation.
 * - future.get() blocks the calling thread until the result is ready.
 */
public class CallableFutureExample {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Callable & Future Example ===\n");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        // --- Example 1: Basic Callable returning a value ---
        System.out.println("--- Example 1: Basic Callable ---");
        Callable<Integer> addTask = () -> {
            System.out.println("Computing on: " + Thread.currentThread().getName());
            Thread.sleep(300);
            return 10 + 20;
        };

        Future<Integer> future = executor.submit(addTask);

        System.out.println("Task submitted. Doing other work...");

        try {
            Integer result = future.get(); // blocks until result is ready
            System.out.println("Result: " + result); // 30
        } catch (ExecutionException e) {
            System.out.println("Task threw an exception: " + e.getCause());
        }

        // --- Example 2: Future with timeout ---
        System.out.println("\n--- Example 2: Future with Timeout ---");
        Future<String> slowFuture = executor.submit(() -> {
            Thread.sleep(2000); // simulate slow task
            return "Slow result";
        });

        try {
            // Wait max 1 second — task takes 2 seconds, so TimeoutException is thrown
            String value = slowFuture.get(1, TimeUnit.SECONDS);
            System.out.println("Got: " + value);
        } catch (TimeoutException e) {
            System.out.println("Timeout! Task took too long.");
            slowFuture.cancel(true); // cancel the still-running task
        } catch (ExecutionException e) {
            System.out.println("Task threw: " + e.getCause());
        }

        // --- Example 3: Checking future status ---
        System.out.println("\n--- Example 3: Future Status ---");
        Future<String> statusFuture = executor.submit(() -> {
            Thread.sleep(200);
            return "Status Result";
        });

        System.out.println("isDone before get: " + statusFuture.isDone());

        try {
            String val = statusFuture.get();
            System.out.println("isDone after get:  " + statusFuture.isDone());
            System.out.println("Result: " + val);
        } catch (ExecutionException e) {
            System.out.println("Error: " + e.getCause());
        }

        // --- Example 4: Exception from Callable ---
        System.out.println("\n--- Example 4: Exception in Callable ---");
        Future<Integer> errorFuture = executor.submit(() -> {
            if (true) throw new RuntimeException("Something went wrong!");
            return 0;
        });

        try {
            errorFuture.get();
        } catch (ExecutionException e) {
            // Exception from Callable is wrapped in ExecutionException
            System.out.println("Caught exception: " + e.getCause().getMessage());
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("\nExecutor shut down.");
    }
}
