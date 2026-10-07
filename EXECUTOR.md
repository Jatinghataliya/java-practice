# ⚙️ Executor & ExecutorService in Java

A comprehensive guide to `Executor` and `ExecutorService` — Java's high-level thread management APIs from the `java.util.concurrent` package introduced in **Java 5**.

---

## 📋 Table of Contents

- [What is Executor?](#what-is-executor)
- [What is ExecutorService?](#what-is-executorservice)
- [Hierarchy](#hierarchy)
- [Creating an ExecutorService](#creating-an-executorservice)
- [execute() vs submit()](#execute-vs-submit)
- [Using Callable and Future](#using-callable-and-future)
- [invokeAll and invokeAny](#invokeall-and-invokeany)
- [Lifecycle Management](#lifecycle-management)
- [ScheduledExecutorService](#scheduledexecutorservice)
- [Virtual Threads (Java 21+)](#virtual-threads-java-21)
- [Executor vs ExecutorService](#executor-vs-executorservice)
- [Best Practices](#best-practices)

---

## What is Executor?

`Executor` is the simplest thread abstraction in Java — a **single-method interface** that decouples task submission from thread management.

```java
public interface Executor {
    void execute(Runnable command);
}
```

### Example

```java
Executor executor = Executors.newSingleThreadExecutor();
executor.execute(() -> System.out.println("Task: " + Thread.currentThread().getName()));
```

> ⚠️ `Executor` only accepts `Runnable` — no return values, no lifecycle control.

---

## What is ExecutorService?

`ExecutorService` **extends `Executor`** and provides a full-featured thread pool with:

- ✅ Task submission with **return values** (`Callable` + `Future`)
- ✅ **Lifecycle management** (`shutdown`, `awaitTermination`)
- ✅ **Batch execution** (`invokeAll`, `invokeAny`)

```java
ExecutorService executor = Executors.newFixedThreadPool(4);

Future<String> future = executor.submit(() -> "Hello from thread!");
System.out.println(future.get()); // Hello from thread!

executor.shutdown();
```

---

## Hierarchy

```
Executor
 └── ExecutorService
      └── ScheduledExecutorService
           └── ScheduledThreadPoolExecutor (concrete class)
      └── ThreadPoolExecutor (concrete class)
      └── ForkJoinPool (concrete class)
```

---

## Creating an ExecutorService

Java provides factory methods via the `Executors` utility class:

```java
// Fixed pool — always exactly N threads
ExecutorService fixed = Executors.newFixedThreadPool(4);

// Single thread — tasks run one at a time, sequentially
ExecutorService single = Executors.newSingleThreadExecutor();

// Cached pool — creates threads on demand, reuses idle ones (unbounded)
ExecutorService cached = Executors.newCachedThreadPool();

// Scheduled — run tasks after a delay or on a fixed schedule
ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(2);

// Virtual threads (Java 21+) — one virtual thread per task
ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor();
```

### Choosing the Right Pool

| Pool Type         | Use When                                              |
|-------------------|-------------------------------------------------------|
| `FixedThreadPool` | Known, stable number of concurrent tasks              |
| `SingleThread`    | Tasks must run sequentially in submission order       |
| `CachedThreadPool`| Many short-lived tasks with unpredictable load        |
| `ScheduledPool`   | Tasks need to run after a delay or periodically       |
| `VirtualThreads`  | High-concurrency I/O-bound tasks (Java 21+)           |

---

## execute() vs submit()

| Feature              | `execute(Runnable)`         | `submit(Runnable / Callable)` |
|----------------------|-----------------------------|-------------------------------|
| Return value         | `void`                      | `Future<?>` or `Future<T>`    |
| Exception handling   | Uncaught, goes to handler   | Captured inside `Future`      |
| Accepts `Callable`   | ❌                          | ✅                            |

```java
ExecutorService executor = Executors.newFixedThreadPool(2);

// execute — fire and forget, exceptions are lost silently
executor.execute(() -> System.out.println("Fire and forget"));

// submit — returns Future, exceptions are captured
Future<Integer> future = executor.submit(() -> 10 + 20);
System.out.println("Result: " + future.get()); // Result: 30

executor.shutdown();
```

---

## Using Callable and Future

`Callable<T>` is like `Runnable` but **returns a value** and can **throw checked exceptions**.

```java
ExecutorService executor = Executors.newFixedThreadPool(3);

Callable<String> task = () -> {
    Thread.sleep(500);
    return "Done by: " + Thread.currentThread().getName();
};

Future<String> future = executor.submit(task);

// future.get() blocks until the result is available
System.out.println(future.get());

// With timeout — throws TimeoutException if not done in time
String result = future.get(2, TimeUnit.SECONDS);

executor.shutdown();
```

### `Future` Methods

| Method                        | Description                                         |
|-------------------------------|-----------------------------------------------------|
| `get()`                       | Blocks until result is available                    |
| `get(timeout, unit)`          | Blocks up to timeout, then throws `TimeoutException`|
| `isDone()`                    | Returns `true` if task completed                    |
| `isCancelled()`               | Returns `true` if task was cancelled                |
| `cancel(mayInterruptIfRunning)`| Attempts to cancel the task                        |

---

## invokeAll and invokeAny

### `invokeAll` — run all, wait for all

```java
ExecutorService executor = Executors.newFixedThreadPool(3);

List<Callable<String>> tasks = List.of(
    () -> "Task A",
    () -> "Task B",
    () -> "Task C"
);

List<Future<String>> results = executor.invokeAll(tasks);

for (Future<String> f : results) {
    System.out.println(f.get());
}

executor.shutdown();
```

### `invokeAny` — run all, return first completed result

```java
ExecutorService executor = Executors.newFixedThreadPool(3);

List<Callable<String>> tasks = List.of(
    () -> { Thread.sleep(300); return "Slow Task"; },
    () -> { Thread.sleep(100); return "Fast Task"; },
    () -> { Thread.sleep(200); return "Medium Task"; }
);

String first = executor.invokeAny(tasks);
System.out.println("First completed: " + first); // Fast Task

executor.shutdown();
```

---

## Lifecycle Management

Always shut down an `ExecutorService` to avoid thread leaks.

```
Running → Shutdown → Terminated
```

```java
ExecutorService executor = Executors.newFixedThreadPool(4);

// Submit tasks
for (int i = 0; i < 10; i++) {
    executor.submit(() -> System.out.println(Thread.currentThread().getName()));
}

// Graceful shutdown — no new tasks accepted, existing tasks finish
executor.shutdown();

try {
    // Wait up to 5 seconds for tasks to finish
    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        // Force stop remaining tasks
        executor.shutdownNow();
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

### Lifecycle Methods

| Method                          | Description                                             |
|---------------------------------|---------------------------------------------------------|
| `shutdown()`                    | Stop accepting new tasks; finish queued tasks           |
| `shutdownNow()`                 | Attempt to stop all tasks immediately; returns pending  |
| `isShutdown()`                  | `true` if `shutdown()` was called                       |
| `isTerminated()`                | `true` if all tasks finished after shutdown             |
| `awaitTermination(time, unit)`  | Block until terminated or timeout expires               |

---

## ScheduledExecutorService

Extends `ExecutorService` to support delayed and periodic task execution.

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

// Run once after 3 seconds
scheduler.schedule(() -> System.out.println("Delayed task"), 3, TimeUnit.SECONDS);

// Run every 2 seconds (fixed rate — based on start time)
scheduler.scheduleAtFixedRate(
    () -> System.out.println("Fixed rate: " + System.currentTimeMillis()),
    0, 2, TimeUnit.SECONDS
);

// Run 2 seconds after previous task completes (fixed delay)
scheduler.scheduleWithFixedDelay(
    () -> System.out.println("Fixed delay task"),
    0, 2, TimeUnit.SECONDS
);
```

| Method                    | Description                                        |
|---------------------------|----------------------------------------------------|
| `schedule()`              | Run once after a delay                             |
| `scheduleAtFixedRate()`   | Run repeatedly at fixed intervals from start time  |
| `scheduleWithFixedDelay()`| Run repeatedly with fixed delay after last finish  |

---

## Virtual Threads (Java 21+)

Virtual threads are lightweight JVM-managed threads ideal for high-concurrency I/O-bound workloads.

```java
// One virtual thread per task — millions can run concurrently
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 1_000_000; i++) {
        executor.submit(() -> {
            Thread.sleep(Duration.ofMillis(100));
            return Thread.currentThread().getName();
        });
    }
} // auto-shutdown via try-with-resources
```

| Feature          | Platform Thread    | Virtual Thread        |
|------------------|--------------------|-----------------------|
| Managed by       | OS                 | JVM                   |
| Memory footprint | ~1 MB stack        | A few KB              |
| Max concurrency  | Thousands          | Millions              |
| Best for         | CPU-bound tasks    | I/O-bound tasks       |

---

## Executor vs ExecutorService

| Feature                  | `Executor`   | `ExecutorService`         |
|--------------------------|--------------|---------------------------|
| Interface methods        | 1            | 10+                       |
| Accepts `Runnable`       | ✅           | ✅                        |
| Accepts `Callable`       | ❌           | ✅                        |
| Returns `Future`         | ❌           | ✅                        |
| `shutdown()` / lifecycle | ❌           | ✅                        |
| `invokeAll` / `invokeAny`| ❌           | ✅                        |
| Use case                 | Simple tasks | Production thread pools   |

---

## Best Practices

- ✅ Always prefer `ExecutorService` over raw `Thread` creation
- ✅ Always call `shutdown()` — never leave a pool running indefinitely
- ✅ Use `awaitTermination()` + `shutdownNow()` for graceful shutdown
- ✅ Use `submit()` over `execute()` — exceptions are captured in `Future`
- ✅ Use `try-with-resources` with `ExecutorService` (Java 19+ supports `AutoCloseable`)
- ✅ Use `FixedThreadPool` for stable workloads, `CachedThreadPool` for bursty ones
- ✅ Avoid `newCachedThreadPool()` for long-running tasks — it can spawn unbounded threads
- ✅ Use `ScheduledExecutorService` instead of `Timer` — it's safer and more flexible
- ✅ Use Virtual Threads (`Java 21+`) for high-concurrency I/O-bound applications

---

## 🛠️ Requirements

- Java 5+ for `Executor`, `ExecutorService`, `ScheduledExecutorService`
- Java 8+ for lambda expressions
- Java 21+ for Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`)

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
