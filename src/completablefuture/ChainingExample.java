package completablefuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * ChainingExample
 *
 * CompletableFuture supports fluent chaining — each step runs after the previous
 * one completes, forming a non-blocking async pipeline.
 *
 * Chaining methods:
 *
 *  thenApply(Function)    — transforms the result (like map). Returns new value.
 *  thenAccept(Consumer)   — consumes the result, returns CompletableFuture<Void>.
 *  thenRun(Runnable)      — runs after completion, ignores the result.
 *
 * Async variants (run next step on a different thread):
 *  thenApplyAsync(fn)
 *  thenAcceptAsync(fn)
 *  thenRunAsync(fn)
 */
public class ChainingExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("=== CompletableFuture Chaining Example ===\n");

        // ── 1. thenApply — transform result step by step ─────────────────────
        System.out.println("--- 1. thenApply() — transform the result ---");

        CompletableFuture<String> pipeline = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("[Step-1] Fetching raw data on: "
                            + Thread.currentThread().getName());
                    sleep(300);
                    return "  raw data  ";
                })
                .thenApply(raw -> {
                    System.out.println("[Step-2] Trimming: '" + raw + "'");
                    return raw.trim();
                })
                .thenApply(trimmed -> {
                    System.out.println("[Step-3] Converting to uppercase: '" + trimmed + "'");
                    return trimmed.toUpperCase();
                })
                .thenApply(upper -> {
                    System.out.println("[Step-4] Adding prefix...");
                    return "RESULT: " + upper;
                });

        System.out.println("[Main] Final: " + pipeline.get());

        // ── 2. thenAccept — consume result, no return ─────────────────────────
        System.out.println("\n--- 2. thenAccept() — consume without returning ---");

        CompletableFuture<Void> consumer = CompletableFuture
                .supplyAsync(() -> {
                    sleep(200);
                    return 42;
                })
                .thenAccept(value ->
                        System.out.println("[Consumer] Received value: " + value + " (no return)"));

        consumer.get(); // wait for consumer to finish

        // ── 3. thenRun — run something after, ignores result ──────────────────
        System.out.println("\n--- 3. thenRun() — run after, ignore result ---");

        CompletableFuture<Void> runner = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("[Task] Computing...");
                    sleep(200);
                    return "some result";
                })
                .thenRun(() ->
                        System.out.println("[Runner] Task completed. Sending notification..."));

        runner.get();

        // ── 4. thenApplyAsync — next step on a DIFFERENT thread ───────────────
        System.out.println("\n--- 4. thenApplyAsync() — force async continuation ---");

        CompletableFuture<String> asyncChain = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("[Step-1] Thread: " + Thread.currentThread().getName());
                    sleep(200);
                    return "data";
                })
                .thenApplyAsync(data -> {
                    // thenApplyAsync submits next step to ForkJoinPool (may be different thread)
                    System.out.println("[Step-2] Thread: " + Thread.currentThread().getName());
                    return data.toUpperCase();
                })
                .thenApplyAsync(upper -> {
                    System.out.println("[Step-3] Thread: " + Thread.currentThread().getName());
                    return "Final: " + upper;
                });

        System.out.println("[Main] " + asyncChain.get());

        // ── 5. Full fluent chain summary ──────────────────────────────────────
        System.out.println("\n--- 5. Combined chain: supply → apply → accept → run ---");

        CompletableFuture
                .supplyAsync(() -> 10)
                .thenApply(n -> n * n)              // 100
                .thenApply(n -> "Value: " + n)       // "Value: 100"
                .thenAccept(s -> System.out.println("[Accept] " + s))
                .thenRun(() -> System.out.println("[Run]    All done!"))
                .get();

        System.out.println("\nChaining examples complete.");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
