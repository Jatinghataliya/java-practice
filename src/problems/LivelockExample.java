package problems;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LivelockExample
 *
 * A Livelock occurs when two or more threads are ACTIVELY RESPONDING to each
 * other's actions but NEITHER makes any real progress — they keep changing state
 * in response to the other, but the work never gets done.
 *
 * Unlike deadlock:
 *   - Deadlock  → threads are BLOCKED (sleeping, waiting for lock)
 *   - Livelock  → threads are RUNNING (active), but stuck in a loop of reactions
 *
 * Classic analogy:
 *   Two people walking towards each other in a hallway.
 *   Both step to the same side to let the other pass.
 *   Then both step to the OTHER side simultaneously.
 *   They keep mirroring each other — both are moving, but neither gets through.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Fix:
 *   1. Random back-off — add randomized delay before retrying
 *   2. Priority / ordering — one thread always yields, the other proceeds
 *   3. Limit retries — after N attempts, one side proceeds unconditionally
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class LivelockExample {

    // ── Hallway analogy — two people trying to pass each other ───────────────
    static class Person {
        private final String name;
        private volatile boolean movingRight;

        Person(String name, boolean movingRight) {
            this.name = name;
            this.movingRight = movingRight;
        }

        boolean isMovingRight() { return movingRight; }

        void stepAside(String direction) {
            System.out.println("  [" + name + "] Stepping " + direction
                    + " to let the other pass...");
            movingRight = !movingRight;
        }

        String getName() { return name; }
    }

    // ── 1. LIVELOCK — both keep stepping to the same side ────────────────────
    static void demonstrateLivelock() throws InterruptedException {
        System.out.println("--- Livelock Demo (hallway analogy, 3 seconds) ---\n");
        System.out.println("Two people walking towards each other. Both keep");
        System.out.println("stepping aside simultaneously — neither gets through.\n");

        Person alice = new Person("Alice", true);   // moving right
        Person bob   = new Person("Bob",   false);  // moving left

        AtomicBoolean livelockActive = new AtomicBoolean(true);
        int[] aliceMoves = {0};
        int[] bobMoves   = {0};

        Thread aliceThread = new Thread(() -> {
            while (livelockActive.get()) {
                // Alice steps aside if she's about to collide with Bob
                if (alice.isMovingRight() == !bob.isMovingRight()) {
                    if (aliceMoves[0] < 6) { // limit prints to avoid flooding
                        alice.stepAside("left ");
                    }
                    aliceMoves[0]++;
                    sleep(300);
                }
            }
        }, "Alice");

        Thread bobThread = new Thread(() -> {
            while (livelockActive.get()) {
                // Bob mirrors Alice — both step the same way every time
                if (!bob.isMovingRight() == alice.isMovingRight()) {
                    if (bobMoves[0] < 6) {
                        bob.stepAside("right");
                    }
                    bobMoves[0]++;
                    sleep(300);
                }
            }
        }, "Bob");

        aliceThread.start();
        bobThread.start();
        Thread.sleep(3000);
        livelockActive.set(false);
        aliceThread.join(500);
        bobThread.join(500);

        System.out.println("\n  Alice moved aside: " + aliceMoves[0] + " times");
        System.out.println("  Bob   moved aside: " + bobMoves[0]   + " times");
        System.out.println("  Neither got through — that's Livelock!\n");
    }

    // ── 2. FIX 1: Random back-off — randomized delay breaks the symmetry ─────
    static void fixWithRandomBackoff() throws InterruptedException {
        System.out.println("--- Fix 1: Random Back-off ---\n");

        Person alice = new Person("Alice", true);
        Person bob   = new Person("Bob",   false);
        AtomicBoolean alicePassed = new AtomicBoolean(false);
        AtomicBoolean bobPassed   = new AtomicBoolean(false);

        Thread aliceThread = new Thread(() -> {
            int attempts = 0;
            while (!alicePassed.get()) {
                if (alice.isMovingRight() != bob.isMovingRight()) {
                    System.out.println("  [Alice] Path is clear! Passing through.");
                    alicePassed.set(true);
                } else {
                    attempts++;
                    System.out.println("  [Alice] Blocked. Waiting random delay (attempt " + attempts + ")...");
                    sleep((long)(Math.random() * 200 + 50)); // random 50–250ms
                    alice.stepAside("left ");
                }
            }
        }, "Alice");

        Thread bobThread = new Thread(() -> {
            int attempts = 0;
            while (!bobPassed.get()) {
                if (bob.isMovingRight() != alice.isMovingRight()) {
                    System.out.println("  [Bob]   Path is clear! Passing through.");
                    bobPassed.set(true);
                } else {
                    attempts++;
                    System.out.println("  [Bob]   Blocked. Waiting random delay (attempt " + attempts + ")...");
                    sleep((long)(Math.random() * 200 + 50)); // different random delay
                    bob.stepAside("right");
                }
            }
        }, "Bob");

        aliceThread.start();
        bobThread.start();
        aliceThread.join(5000);
        bobThread.join(5000);

        System.out.println("  Both passed! Livelock resolved by random back-off.\n");
    }

    // ── 3. FIX 2: Priority ordering — one always yields, other proceeds ───────
    static void fixWithPriority() throws InterruptedException {
        System.out.println("--- Fix 2: Priority Ordering (Alice always has right of way) ---\n");

        AtomicBoolean alicePassed = new AtomicBoolean(false);
        AtomicBoolean bobPassed   = new AtomicBoolean(false);
        AtomicBoolean aliceReady  = new AtomicBoolean(true);

        Thread aliceThread = new Thread(() -> {
            System.out.println("  [Alice] Has priority — moving through directly.");
            sleep(100);
            alicePassed.set(true);
            aliceReady.set(false); // signals Bob to go after Alice
            System.out.println("  [Alice] Passed!");
        }, "Alice");

        Thread bobThread = new Thread(() -> {
            // Bob waits until Alice has passed
            System.out.println("  [Bob]   Yielding to Alice (lower priority)...");
            while (aliceReady.get()) { sleep(50); } // spin-wait
            System.out.println("  [Bob]   Alice passed. Bob moving through.");
            sleep(100);
            bobPassed.set(true);
            System.out.println("  [Bob]   Passed!");
        }, "Bob");

        aliceThread.start();
        bobThread.start();
        aliceThread.join();
        bobThread.join();

        System.out.println("  Both passed in order. Livelock resolved by priority.\n");
    }

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws InterruptedException {

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║         LIVELOCK Example             ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        System.out.println("Livelock: threads are ACTIVE (not blocked) but keep");
        System.out.println("reacting to each other — no real progress is made.\n");

        demonstrateLivelock();
        fixWithRandomBackoff();
        fixWithPriority();

        System.out.println("══════════════════════════════════════════");
        System.out.println("Livelock vs Deadlock:");
        System.out.println("  Deadlock  → threads BLOCKED  (sleeping, waiting for lock)");
        System.out.println("  Livelock  → threads ACTIVE   (running, but making no progress)");
        System.out.println();
        System.out.println("Fixes:");
        System.out.println("  1. Random back-off  — randomized delay breaks symmetry");
        System.out.println("  2. Priority ordering — one side always proceeds first");
        System.out.println("  3. Retry limit       — after N attempts, one side forces through");
        System.out.println("══════════════════════════════════════════");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
