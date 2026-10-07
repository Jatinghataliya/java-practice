package completablefuture;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * CombiningExample
 *
 * CompletableFuture supports combining multiple async tasks:
 *
 *  thenCombine(other, BiFunction) — combine results of TWO independent futures.
 *  thenCompose(Function)          — chain futures where second depends on first (flatMap).
 *  allOf(futures...)              — wait for ALL futures to complete.
 *  anyOf(futures...)              — wait for ANY ONE future to complete (first wins).
 */
public class CombiningExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("=== CompletableFuture Combining Example ===\n");

        // ── 1. thenCombine — merge two independent futures ────────────────────
        System.out.println("--- 1. thenCombine() — merge 2 independent tasks ---");

        CompletableFuture<Integer> priceTask = CompletableFuture.supplyAsync(() -> {
            System.out.println("[Price]    Fetching product price...");
            sleep(500);
            return 1200;
        });

        CompletableFuture<Integer> discountTask = CompletableFuture.supplyAsync(() -> {
            System.out.println("[Discount] Fetching discount...");
            sleep(300);
            return 200;
        });

        // Both run in parallel; combine once both finish
        CompletableFuture<Integer> finalPrice = priceTask.thenCombine(discountTask,
                (price, discount) -> {
                    System.out.println("[Combine]  price=" + price + ", discount=" + discount);
                    return price - discount;
                });

        System.out.println("[Main] Final price: $" + finalPrice.get() + "\n");

        // ── 2. thenCompose — chain where step 2 depends on step 1's result ───
        System.out.println("--- 2. thenCompose() — dependent async chain (flatMap) ---");

        CompletableFuture<String> composed = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("[Step-1] Fetching user ID...");
                    sleep(300);
                    return "user-42";
                })
                .thenCompose(userId -> {
                    // Use userId to fetch profile — returns another CompletableFuture
                    System.out.println("[Step-2] Fetching profile for: " + userId);
                    return CompletableFuture.supplyAsync(() -> {
                        sleep(300);
                        return "Profile{id=" + userId + ", name=Jatin}";
                    });
                })
                .thenCompose(profile -> {
                    System.out.println("[Step-3] Fetching settings for: " + profile);
                    return CompletableFuture.supplyAsync(() -> {
                        sleep(200);
                        return profile + ", settings=dark-mode";
                    });
                });

        System.out.println("[Main] " + composed.get() + "\n");

        // ── 3. allOf — wait for ALL futures to complete ───────────────────────
        System.out.println("--- 3. allOf() — wait for ALL to complete ---");

        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> { sleep(400); return "Service-A"; });
        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> { sleep(200); return "Service-B"; });
        CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> { sleep(600); return "Service-C"; });

        // allOf returns CompletableFuture<Void> — collect results separately
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(task1, task2, task3);

        allTasks.thenRun(() -> {
            try {
                System.out.println("[allOf] All done:");
                System.out.println("  " + task1.get());
                System.out.println("  " + task2.get());
                System.out.println("  " + task3.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).get();

        // ── 4. allOf with stream — collect all results into a List ────────────
        System.out.println("\n--- 4. allOf + stream to collect all results ---");

        List<CompletableFuture<String>> futures = Arrays.asList(
                CompletableFuture.supplyAsync(() -> { sleep(300); return "Alpha"; }),
                CompletableFuture.supplyAsync(() -> { sleep(100); return "Beta"; }),
                CompletableFuture.supplyAsync(() -> { sleep(200); return "Gamma"; })
        );

        CompletableFuture<List<String>> allResults = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join) // join() = get() without checked exception
                        .collect(Collectors.toList()));

        System.out.println("[allOf+stream] Results: " + allResults.get());

        // ── 5. anyOf — return first completed result ──────────────────────────
        System.out.println("\n--- 5. anyOf() — first one wins ---");

        CompletableFuture<Object> fastest = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> { sleep(500); return "Slow  (500ms)"; }),
                CompletableFuture.supplyAsync(() -> { sleep(100); return "Fast  (100ms)"; }),
                CompletableFuture.supplyAsync(() -> { sleep(300); return "Medium(300ms)"; })
        );

        System.out.println("[anyOf] First to finish: " + fastest.get());

        System.out.println("\nCombining examples complete.");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
