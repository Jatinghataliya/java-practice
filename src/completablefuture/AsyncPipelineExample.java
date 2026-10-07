package completablefuture;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * AsyncPipelineExample
 *
 * A real-world inspired async pipeline using CompletableFuture.
 *
 * Scenario: E-Commerce Order Processing
 *  Step 1: Validate order         (async)
 *  Step 2: Check inventory        (async, depends on step 1)
 *  Step 3: Fetch price & discount (async, parallel — independent)
 *  Step 4: Calculate total        (combine price + discount)
 *  Step 5: Process payment        (async, depends on step 4)
 *  Step 6: Send confirmation      (async, final step)
 *
 * Error recovery: if payment fails, log and return "FAILED" status.
 */
public class AsyncPipelineExample {

    // Simulated service layer
    static class OrderService {

        static CompletableFuture<String> validateOrder(String orderId) {
            return CompletableFuture.supplyAsync(() -> {
                log("Validating order: " + orderId);
                sleep(200);
                if (orderId == null || orderId.isEmpty())
                    throw new RuntimeException("Invalid order ID");
                return orderId;
            });
        }

        static CompletableFuture<Integer> checkInventory(String orderId) {
            return CompletableFuture.supplyAsync(() -> {
                log("Checking inventory for: " + orderId);
                sleep(300);
                return 5; // 5 units available
            });
        }

        static CompletableFuture<Double> fetchPrice(String orderId) {
            return CompletableFuture.supplyAsync(() -> {
                log("Fetching price for: " + orderId);
                sleep(400);
                return 1500.00;
            });
        }

        static CompletableFuture<Double> fetchDiscount(String orderId) {
            return CompletableFuture.supplyAsync(() -> {
                log("Fetching discount for: " + orderId);
                sleep(250);
                return 150.00;
            });
        }

        static CompletableFuture<String> processPayment(double amount) {
            return CompletableFuture.supplyAsync(() -> {
                log("Processing payment of $" + amount);
                sleep(500);
                // Simulate occasional failure
                // if (Math.random() < 0.3) throw new RuntimeException("Payment gateway timeout!");
                return "PAY-" + System.currentTimeMillis();
            });
        }

        static CompletableFuture<String> sendConfirmation(String paymentId, String orderId) {
            return CompletableFuture.supplyAsync(() -> {
                log("Sending confirmation for order: " + orderId + ", payment: " + paymentId);
                sleep(150);
                return "Order " + orderId + " confirmed! Payment: " + paymentId;
            });
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("=== Async Order Processing Pipeline ===\n");
        long start = System.currentTimeMillis();

        String orderId = "ORD-2024-001";

        // ── Pipeline ──────────────────────────────────────────────────────────
        CompletableFuture<String> orderPipeline = OrderService
                // Step 1: Validate
                .validateOrder(orderId)

                // Step 2: Check inventory (depends on valid orderId)
                .thenCompose(validId -> OrderService.checkInventory(validId)
                        .thenApply(stock -> {
                            log("Stock available: " + stock + " units");
                            if (stock <= 0) throw new RuntimeException("Out of stock!");
                            return validId;
                        }))

                // Steps 3 & 4: Fetch price + discount IN PARALLEL, then combine
                .thenCompose(validId -> {
                    CompletableFuture<Double> priceFuture    = OrderService.fetchPrice(validId);
                    CompletableFuture<Double> discountFuture = OrderService.fetchDiscount(validId);

                    return priceFuture.thenCombine(discountFuture, (price, discount) -> {
                        double total = price - discount;
                        log("Price: $" + price + ", Discount: $" + discount
                                + ", Total: $" + total);
                        return total;
                    });
                })

                // Step 5: Process payment
                .thenCompose(OrderService::processPayment)

                // Step 6: Send confirmation
                .thenCompose(paymentId -> OrderService.sendConfirmation(paymentId, orderId))

                // Error recovery
                .exceptionally(ex -> {
                    log("Pipeline failed: " + ex.getMessage());
                    return "ORDER FAILED: " + ex.getMessage();
                });

        String result = orderPipeline.get();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("\n=============================");
        System.out.println("Result:  " + result);
        System.out.println("Time:    " + elapsed + "ms");
        System.out.println("=============================");

        // ── Bonus: Parallel order processing ─────────────────────────────────
        System.out.println("\n=== Bonus: Process multiple orders in parallel ===\n");

        List<String> orderIds = Arrays.asList("ORD-001", "ORD-002", "ORD-003");
        long parallelStart = System.currentTimeMillis();

        List<CompletableFuture<String>> orderFutures = orderIds.stream()
                .map(id -> OrderService.validateOrder(id)
                        .thenCompose(OrderService::checkInventory)
                        .thenCompose(stock -> OrderService.fetchPrice(id))
                        .thenCompose(OrderService::processPayment)
                        .thenCompose(payId -> OrderService.sendConfirmation(payId, id))
                        .exceptionally(ex -> "FAILED: " + id + " — " + ex.getMessage()))
                .collect(Collectors.toList());

        List<String> results = CompletableFuture
                .allOf(orderFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> orderFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()))
                .get();

        long parallelElapsed = System.currentTimeMillis() - parallelStart;

        System.out.println("All orders processed:");
        results.forEach(r -> System.out.println("  ✓ " + r));
        System.out.println("Total time (parallel): " + parallelElapsed + "ms");
    }

    private static void log(String msg) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + msg);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
