package communication;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * SemaphoreExample
 *
 * Semaphore controls access to a shared resource by maintaining a set of permits.
 *
 * acquire() — takes a permit (blocks if none available).
 * release() — returns a permit (wakes a waiting thread).
 *
 * Semaphore(1)  → acts like a mutex (binary semaphore).
 * Semaphore(N)  → allows up to N threads to access a resource concurrently.
 *
 * Use cases:
 *  - Limiting concurrent DB connections.
 *  - Rate limiting (max N tasks at a time).
 *  - Parking lot / resource pool simulation.
 */
public class SemaphoreExample {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Semaphore Example ===\n");

        // ── Example 1: Parking lot — only 3 spots available ───────────────────
        System.out.println("--- Example 1: Parking Lot (3 spots, 8 cars) ---\n");

        Semaphore parkingLot = new Semaphore(3); // only 3 permits
        ExecutorService executor = Executors.newFixedThreadPool(8);

        for (int i = 1; i <= 8; i++) {
            final int carId = i;
            executor.submit(() -> {
                try {
                    System.out.println("[Car-" + carId + "] Trying to park...");
                    parkingLot.acquire(); // wait for a free spot

                    System.out.println("[Car-" + carId + "] Parked!  Available spots: "
                            + parkingLot.availablePermits());
                    Thread.sleep(1000); // stay parked
                    System.out.println("[Car-" + carId + "] Leaving. Available spots: "
                            + (parkingLot.availablePermits() + 1));

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    parkingLot.release(); // free the spot
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        // ── Example 2: Binary semaphore as mutex ──────────────────────────────
        System.out.println("\n--- Example 2: Binary Semaphore as Mutex ---\n");

        Semaphore mutex = new Semaphore(1); // only 1 permit = mutex
        int[] sharedCounter = {0};

        ExecutorService executor2 = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 5; i++) {
            executor2.submit(() -> {
                try {
                    mutex.acquire();
                    // critical section — only 1 thread at a time
                    int current = sharedCounter[0];
                    Thread.sleep(50);
                    sharedCounter[0] = current + 1;
                    System.out.println("[" + Thread.currentThread().getName()
                            + "] Counter = " + sharedCounter[0]);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    mutex.release();
                }
            });
        }

        executor2.shutdown();
        executor2.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("\nFinal counter value: " + sharedCounter[0] + " (expected 5)");

        // ── Example 3: tryAcquire — non-blocking attempt ──────────────────────
        System.out.println("\n--- Example 3: tryAcquire() --- non-blocking ---");
        Semaphore limited = new Semaphore(2);

        for (int i = 1; i <= 5; i++) {
            final int id = i;
            new Thread(() -> {
                if (limited.tryAcquire()) {
                    try {
                        System.out.println("[Task-" + id + "] Acquired permit. Working...");
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        limited.release();
                    }
                } else {
                    System.out.println("[Task-" + id + "] No permit available. Skipping.");
                }
            }).start();
        }

        Thread.sleep(1000);
        System.out.println("\nDone.");
    }
}
