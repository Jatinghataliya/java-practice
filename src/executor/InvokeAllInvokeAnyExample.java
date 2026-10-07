package executor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * InvokeAllInvokeAnyExample
 *
 * Demonstrates invokeAll() and invokeAny() on ExecutorService.
 *
 * invokeAll()  — Submits all tasks, blocks until ALL complete, returns List<Future<T>>
 * invokeAny()  — Submits all tasks, returns the result of the FIRST one to complete,
 *                cancels the rest.
 */
public class InvokeAllInvokeAnyExample {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        System.out.println("=== invokeAll & invokeAny Example ===\n");

        ExecutorService executor = Executors.newFixedThreadPool(4);

        // --- Example 1: invokeAll ---
        System.out.println("--- invokeAll: Wait for ALL tasks ---");

        List<Callable<String>> allTasks = Arrays.asList(
            () -> { Thread.sleep(300); return "Task A completed"; },
            () -> { Thread.sleep(100); return "Task B completed"; },
            () -> { Thread.sleep(200); return "Task C completed"; },
            () -> { Thread.sleep(400); return "Task D completed"; }
        );

        // Blocks until ALL 4 tasks are done
        List<Future<String>> results = executor.invokeAll(allTasks);

        System.out.println("All tasks finished:");
        for (Future<String> f : results) {
            System.out.println("  " + f.get()); // guaranteed done — no blocking here
        }

        // --- Example 2: invokeAny ---
        System.out.println("\n--- invokeAny: Return FIRST completed task ---");

        List<Callable<String>> anyTasks = Arrays.asList(
            () -> { Thread.sleep(500); return "Slow Server (500ms)"; },
            () -> { Thread.sleep(100); return "Fast Server (100ms)"; },
            () -> { Thread.sleep(300); return "Medium Server (300ms)"; }
        );

        // Returns the result of the fastest task; cancels the rest
        String fastest = executor.invokeAny(anyTasks);
        System.out.println("First to respond: " + fastest);

        // --- Example 3: invokeAll with one failing task ---
        System.out.println("\n--- invokeAll with a failing task ---");

        List<Callable<Integer>> mixedTasks = Arrays.asList(
            () -> { Thread.sleep(100); return 100; },
            () -> { throw new RuntimeException("Task failed!"); },
            () -> { Thread.sleep(200); return 300; }
        );

        List<Future<Integer>> mixedResults = executor.invokeAll(mixedTasks);

        for (int i = 0; i < mixedResults.size(); i++) {
            try {
                System.out.println("Task " + (i + 1) + " result: " + mixedResults.get(i).get());
            } catch (ExecutionException e) {
                System.out.println("Task " + (i + 1) + " failed: " + e.getCause().getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("\nExecutor shut down.");
    }
}
