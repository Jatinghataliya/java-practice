package communication;

/**
 * WaitNotifyExample
 *
 * Demonstrates inter-thread communication using wait() and notify().
 *
 * wait()   — releases the lock and puts the thread into WAITING state.
 * notify() — wakes up ONE thread that is waiting on the same object's monitor.
 *
 * Rules:
 *  - wait() and notify() must be called inside a synchronized block/method.
 *  - Always call wait() inside a loop (not if), to guard against spurious wakeups.
 */
public class WaitNotifyExample {

    private static final Object lock = new Object();
    private static boolean dataReady = false;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== wait() & notify() Example ===\n");

        // Waiter thread — waits until data is ready
        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                System.out.println("[Waiter] Acquired lock. Checking if data is ready...");

                while (!dataReady) {          // loop guards against spurious wakeups
                    try {
                        System.out.println("[Waiter] Data not ready. Calling wait()...");
                        lock.wait();          // releases lock, enters WAITING state
                        System.out.println("[Waiter] Woken up! Re-checking condition...");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                System.out.println("[Waiter] Data is ready! Processing data.");
            }
        }, "Waiter-Thread");

        // Notifier thread — prepares data and notifies waiter
        Thread notifier = new Thread(() -> {
            try {
                Thread.sleep(2000); // simulate time to prepare data
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            synchronized (lock) {
                System.out.println("[Notifier] Data prepared. Setting dataReady = true.");
                dataReady = true;
                lock.notify(); // wake up the waiting thread
                System.out.println("[Notifier] notify() called. Releasing lock now.");
            }
        }, "Notifier-Thread");

        waiter.start();
        Thread.sleep(100); // ensure waiter enters wait() before notifier runs
        notifier.start();

        waiter.join();
        notifier.join();

        System.out.println("\nBoth threads finished.");
    }
}
