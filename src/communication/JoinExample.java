package communication;

/**
 * JoinExample
 *
 * Demonstrates thread.join() for inter-thread coordination.
 *
 * join()           — current thread waits INDEFINITELY for the target thread to finish.
 * join(millis)     — current thread waits at most millis ms for the target to finish.
 *
 * Use join() when:
 *  - A thread must complete before another can begin.
 *  - You need to aggregate results from multiple threads sequentially.
 */
public class JoinExample {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== join() Example ===\n");

        // ── Example 1: Basic join — wait for one thread ───────────────────────
        System.out.println("--- Example 1: Basic join() ---");

        Thread downloader = new Thread(() -> {
            System.out.println("[Downloader] Downloading file...");
            try { Thread.sleep(2000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("[Downloader] Download complete!");
        }, "Downloader");

        downloader.start();
        System.out.println("[Main] Waiting for downloader to finish...");
        downloader.join(); // main thread blocks here until downloader is done
        System.out.println("[Main] Downloader finished. Processing file now.\n");

        // ── Example 2: Chain of threads ───────────────────────────────────────
        System.out.println("--- Example 2: Chained thread execution ---");

        Thread step1 = new Thread(() -> {
            System.out.println("[Step-1] Fetching data...");
            try { Thread.sleep(500); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("[Step-1] Done.");
        });

        Thread step2 = new Thread(() -> {
            System.out.println("[Step-2] Processing data...");
            try { Thread.sleep(500); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("[Step-2] Done.");
        });

        Thread step3 = new Thread(() -> {
            System.out.println("[Step-3] Saving results...");
            try { Thread.sleep(500); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("[Step-3] Done.");
        });

        step1.start();
        step1.join();  // step2 only starts after step1 completes

        step2.start();
        step2.join();  // step3 only starts after step2 completes

        step3.start();
        step3.join();

        System.out.println("[Main] All steps completed in order.\n");

        // ── Example 3: join with timeout ──────────────────────────────────────
        System.out.println("--- Example 3: join(timeout) ---");

        Thread slowTask = new Thread(() -> {
            System.out.println("[SlowTask] Starting very slow task...");
            try { Thread.sleep(5000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("[SlowTask] Done.");
        });

        slowTask.start();
        slowTask.join(1000); // wait max 1 second

        if (slowTask.isAlive()) {
            System.out.println("[Main] SlowTask still running after 1s. Moving on...");
            slowTask.interrupt(); // optionally interrupt it
        } else {
            System.out.println("[Main] SlowTask finished in time.");
        }

        System.out.println("\nMain thread finished.");
    }
}
