package completablefuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BasicCompletableFutureExample
 *
 * CompletableFuture (Java 8+) is an enhanced Future that supports:
 *  - Non-blocking async computation
 *  - Chaining and composition of tasks
 *  - Exception handling
 *  - Manual completion
 *
 * Core factory methods:
 *  supplyAsync(Supplier)  — runs async task that RETURNS a value.
 *  runAsync(Runnable)     — runs async task with NO return value.
 *  completedFuture(value) — already-completed future with a given value.
 *
 * By default, supplyAsync/runAsync use ForkJoinPool.commonPool().
 * You can supply a custom Executor as the second argument.
 */
public class BasicCompletableFutureExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("=== Basic CompletableFuture Example ===\n");

        // ── 1. supplyAsync — async task that returns a value ─────────────────
        System.out.println("--- 1. supplyAsync() ---");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("[Task] Running on: " + Thread.currentThread().getName());
            sleep(500);
            return "Hello from CompletableFuture!";
        });

        System.out.println("[Main] Doing other work while task runs...");
        String result = future.get(); // blocks until result is ready
        System.out.println("[Main] Result: " + result);

        // ── 2. runAsync — async task with no return value ─────────────────────
        System.out.println("\n--- 2. runAsync() ---");

        CompletableFuture<Void> voidFuture = CompletableFuture.runAsync(() -> {
            System.out.println("[Task] Fire-and-forget on: " + Thread.currentThread().getName());
            sleep(300);
            System.out.println("[Task] Done.");
        });

        voidFuture.get(); // wait for it to finish
        System.out.println("[Main] runAsync completed.");

        // ── 3. completedFuture — already-done future (no async) ──────────────
        System.out.println("\n--- 3. completedFuture() ---");

        CompletableFuture<Integer> doneFuture = CompletableFuture.completedFuture(42);
        System.out.println("isDone: " + doneFuture.isDone());
        System.out.println("Value:  " + doneFuture.get()); // returns immediately

        // ── 4. supplyAsync with a custom Executor ────────────────────────────
        System.out.println("\n--- 4. supplyAsync with custom Executor ---");

        ExecutorService customPool = Executors.newFixedThreadPool(2);

        CompletableFuture<Integer> customFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("[Task] Running on: " + Thread.currentThread().getName());
            sleep(200);
            return 100 + 200;
        }, customPool);

        System.out.println("[Main] Result from custom pool: " + customFuture.get());
        customPool.shutdown();

        // ── 5. Manual completion with complete() ─────────────────────────────
        System.out.println("\n--- 5. Manual complete() ---");

        CompletableFuture<String> manualFuture = new CompletableFuture<>();

        // Another thread manually completes the future
        new Thread(() -> {
            sleep(500);
            System.out.println("[Thread] Completing the future manually...");
            manualFuture.complete("Manually completed!"); // sets result
        }).start();

        System.out.println("[Main] Waiting for manual completion...");
        System.out.println("[Main] Got: " + manualFuture.get());

        // ── 6. getNow — non-blocking with fallback ────────────────────────────
        System.out.println("\n--- 6. getNow(fallback) ---");

        CompletableFuture<String> fastFuture = CompletableFuture.supplyAsync(() -> {
            sleep(2000); // takes 2 seconds
            return "Late result";
        });

        // getNow returns fallback immediately if result isn't ready yet
        String nowResult = fastFuture.getNow("Not ready yet");
        System.out.println("getNow result: " + nowResult); // prints fallback

        System.out.println("\nDone.");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
