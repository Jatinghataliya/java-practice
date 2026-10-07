package completablefuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * ExceptionHandlingExample
 *
 * CompletableFuture provides three ways to handle exceptions in async pipelines:
 *
 *  exceptionally(fn)    — catch an exception and provide a fallback value.
 *                         Only called if an exception occurred.
 *
 *  handle(BiFunction)   — always called (success or failure).
 *                         Receives (result, exception) — one will be null.
 *
 *  whenComplete(BiConsumer) — always called (success or failure).
 *                             Can inspect but NOT transform the result.
 *                             Exception still propagates after this.
 */
public class ExceptionHandlingExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("=== CompletableFuture Exception Handling ===\n");

        // ── 1. exceptionally — provide a fallback on failure ─────────────────
        System.out.println("--- 1. exceptionally() — fallback on error ---");

        CompletableFuture<String> withFallback = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("[Task] Simulating a failure...");
                    if (true) throw new RuntimeException("Service unavailable!");
                    return "real data";
                })
                .exceptionally(ex -> {
                    System.out.println("[Fallback] Caught: " + ex.getMessage());
                    return "default data"; // fallback value
                });

        System.out.println("[Main] Result: " + withFallback.get() + "\n");

        // ── 2. exceptionally — no error case (not called on success) ─────────
        System.out.println("--- 2. exceptionally() — skipped on success ---");

        CompletableFuture<String> success = CompletableFuture
                .supplyAsync(() -> "real data")
                .exceptionally(ex -> {
                    System.out.println("[Fallback] This should NOT print.");
                    return "fallback";
                });

        System.out.println("[Main] Result: " + success.get() + "\n");

        // ── 3. handle — always called, transforms result or exception ─────────
        System.out.println("--- 3. handle() — always called (success & failure) ---");

        // Case A: handle on failure
        CompletableFuture<String> handledFailure = CompletableFuture
                .<String>supplyAsync(() -> {
                    throw new RuntimeException("DB connection failed!");
                })
                .handle((result, ex) -> {
                    if (ex != null) {
                        System.out.println("[Handle-A] Exception: " + ex.getMessage());
                        return "RECOVERED";
                    }
                    return result;
                });

        System.out.println("[Main] handle on failure: " + handledFailure.get());

        // Case B: handle on success
        CompletableFuture<String> handledSuccess = CompletableFuture
                .supplyAsync(() -> "good data")
                .handle((result, ex) -> {
                    if (ex != null) {
                        return "RECOVERED";
                    }
                    System.out.println("[Handle-B] Success: " + result);
                    return result.toUpperCase();
                });

        System.out.println("[Main] handle on success: " + handledSuccess.get() + "\n");

        // ── 4. whenComplete — observe result/exception, cannot transform ──────
        System.out.println("--- 4. whenComplete() — side-effect only ---");

        CompletableFuture<Integer> observed = CompletableFuture
                .supplyAsync(() -> {
                    sleep(200);
                    return 42;
                })
                .whenComplete((result, ex) -> {
                    // called for both success and failure
                    if (ex != null) {
                        System.out.println("[whenComplete] Error: " + ex.getMessage());
                    } else {
                        System.out.println("[whenComplete] Observed result: " + result);
                    }
                    // cannot change result here — use handle() for that
                });

        System.out.println("[Main] Final value: " + observed.get() + "\n");

        // ── 5. Exception propagation through a chain ──────────────────────────
        System.out.println("--- 5. Exception propagates through thenApply chain ---");

        CompletableFuture<String> chain = CompletableFuture
                .supplyAsync(() -> {
                    throw new RuntimeException("Step 1 failed!");
                })
                .thenApply(v -> "Step 2: " + v)   // SKIPPED — exception propagates
                .thenApply(v -> "Step 3: " + v)   // SKIPPED
                .exceptionally(ex -> {
                    System.out.println("[Catch] Exception caught at end: " + ex.getMessage());
                    return "Pipeline recovered";
                });

        System.out.println("[Main] Chain result: " + chain.get());

        // ── 6. completeExceptionally — manually fail a future ────────────────
        System.out.println("\n--- 6. completeExceptionally() ---");

        CompletableFuture<String> manual = new CompletableFuture<>();
        manual.completeExceptionally(new RuntimeException("Manually failed!"));

        String manualResult = manual
                .exceptionally(ex -> "Caught manual failure: " + ex.getMessage())
                .get();

        System.out.println("[Main] " + manualResult);

        System.out.println("\nException handling examples complete.");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
