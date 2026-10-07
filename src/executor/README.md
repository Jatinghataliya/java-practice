# ⚙️ Executor & ExecutorService in Java

A complete reference for `Executor`, `ExecutorService`, and `ScheduledExecutorService` — Java's high-level thread management APIs from `java.util.concurrent` introduced in **Java 5**.

---

## 📋 Table of Contents

- [What is Executor?](#what-is-executor)
- [What is ExecutorService?](#what-is-executorservice)
- [Why use Executor framework?](#why-use-executor-framework)
- [Raw Thread vs Executor](#raw-thread-vs-executor)
- [Hierarchy](#hierarchy)
- [Creating an ExecutorService](#creating-an-executorservice)
- [execute() vs submit()](#execute-vs-submit)
- [Callable and Future](#callable-and-future)
- [ExecutorService Methods Reference](#executorservice-methods-reference)
- [ScheduledExecutorService Methods Reference](#scheduledexecutorservice-methods-reference)
- [Lifecycle Management](#lifecycle-management)
- [Executor vs ExecutorService vs ScheduledExecutorService](#executor-vs-executorservice-vs-scheduledexecutorservice)
- [Thread Pool Types Comparison](#thread-pool-types-comparison)
- [Real-World Use Cases](#real-world-use-cases)
- [Best Practices](#best-practices)
- [Examples in this folder](#examples-in-this-folder)

---

## What is Executor?

`Executor` is the simplest thread abstraction — a **single-method interface** that decouples task submission from thread management.

```java
public interface Executor {
    void execute(Runnable command);
}
```

Instead of manually creating threads:
```java
// ❌ Raw thread — tightly coupled
new Thread(() -> System.out.println("task")).start();

// ✅ Executor — decoupled
Executor executor = Executors.newSingleThreadExecutor();
executor.execute(() -> System.out.println("task"));
```

> `Executor` only accepts `Runnable` — no return values, no lifecycle control.

---

## What is ExecutorService?

`ExecutorService` **extends `Executor`** and adds a full-featured thread pool with:

- ✅ Task submission with **return values** via `Callable` + `Future`
- ✅ **Lifecycle management** — `shutdown()`, `awaitTermination()`
- ✅ **Batch execution** — `invokeAll()`, `invokeAny()`

```java
ExecutorService executor = Executors.newFixedThreadPool(4);

Future<String> future = executor.submit(() -> "Hello from thread!");
System.out.println(future.get()); // Hello from thread!

executor.shutdown();
```

---

## Why use Executor framework?

### Problems with raw `Thread`:

| Problem | Description |
|---|---|
| No reuse | Each `new Thread()` creates and destroys an OS thread — expensive |
| No control | No limit on how many threads are created |
| No return values | `Runnable.run()` returns void |
| No lifecycle | No built-in way to wait for all tasks to finish |
| No scheduling | No built-in delay or periodic execution |
| Hard to manage | Exceptions are hard to catch across threads |

### How Executor framework solves it:

| Solution | API |
|---|---|
| Thread reuse (pooling) | `FixedThreadPool`, `CachedThreadPool` |
| Thread count control | Pool size limits, queue-based backpressure |
| Return values | `Callable<T>` + `Future<T>` |
| Lifecycle management | `shutdown()`, `awaitTermination()`, `isTerminated()` |
| Scheduling | `ScheduledExecutorService` |
| Exception capturing | `Future.get()` wraps exceptions in `ExecutionException` |

---

## Raw Thread vs Executor

```java
// ❌ Raw Thread — no pooling, no lifecycle, no return value
for (int i = 0; i < 100; i++) {
    new Thread(() -> processTask()).start(); // 100 OS threads!
}

// ✅ ExecutorService — pooled, controlled, managed
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 100; i++) {
    executor.submit(() -> processTask()); // max 10 threads, 90 queued
}
executor.shutdown();
```

---

## Hierarchy

```
Executor                        (java.util.concurrent)
 └── ExecutorService            (java.util.concurrent)
      └── ScheduledExecutorService  (java.util.concurrent)

Concrete implementations:
  ThreadPoolExecutor            — general-purpose pool (backing most Executors factories)
  ScheduledThreadPoolExecutor   — scheduled pool
  ForkJoinPool                  — work-stealing pool (used by CompletableFuture)
```

---

## Creating an ExecutorService

All factory methods are in the `Executors` utility class:

### `newFixedThreadPool(int n)`
Fixed number of threads. Tasks beyond N are queued.
```java
ExecutorService executor = Executors.newFixedThreadPool(4);
```
**Best for:** Stable workloads with a known max concurrency.

---

### `newSingleThreadExecutor()`
Exactly one thread. Tasks execute sequentially in submission order.
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
```
**Best for:** Ordered tasks, single-threaded event loops, sequential DB writes.

---

### `newCachedThreadPool()`
Creates threads on demand. Reuses idle threads. No upper bound.
```java
ExecutorService executor = Executors.newCachedThreadPool();
```
**Best for:** Many short-lived, bursty tasks.
⚠️ Avoid for long-running tasks — unbounded thread creation.

---

### `newScheduledThreadPool(int n)`
Supports delayed and periodic task execution.
```java
ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
```
**Best for:** Cron-like jobs, retry with delay, periodic health checks.

---

### `newVirtualThreadPerTaskExecutor()` *(Java 21+)*
Creates one virtual thread per task. Millions of tasks possible.
```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```
**Best for:** High-concurrency I/O-bound workloads.

---

### Custom `ThreadPoolExecutor`
Full control over pool size, queue, and rejection policy.
```java
ExecutorService executor = new ThreadPoolExecutor(
    2,                              // corePoolSize
    10,                             // maximumPoolSize
    60L, TimeUnit.SECONDS,          // keepAliveTime
    new LinkedBlockingQueue<>(100), // workQueue
    new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
);
```

---

## execute() vs submit()

| Feature | `execute(Runnable)` | `submit(Runnable / Callable)` |
|---|---|---|
| Inherited from | `Executor` | `ExecutorService` |
| Return value | `void` | `Future<?>` or `Future<T>` |
| Exception handling | Uncaught, goes to `UncaughtExceptionHandler` | Captured inside `Future` |
| Accepts `Callable` | ❌ | ✅ |
| Best for | Fire-and-forget | When you need result or exception |

```java
ExecutorService executor = Executors.newFixedThreadPool(2);

// execute — no future, exception silently lost
executor.execute(() -> System.out.println("Fire and forget"));

// submit — returns Future, exception captured
Future<Integer> future = executor.submit(() -> 10 + 20);
System.out.println("Result: " + future.get()); // 30

executor.shutdown();
```

---

## Callable and Future

`Callable<T>` is like `Runnable` but returns a value and can throw checked exceptions.

```java
Callable<String> task = () -> {
    Thread.sleep(500);
    return "Done by: " + Thread.currentThread().getName();
};

ExecutorService executor = Executors.newFixedThreadPool(2);
Future<String> future = executor.submit(task);

System.out.println(future.get());                          // blocks until ready
String result = future.get(2, TimeUnit.SECONDS);           // with timeout
executor.shutdown();
```

### `Future<T>` Methods

| Method | Description |
|---|---|
| `get()` | Blocks until result is available; throws `ExecutionException`, `InterruptedException` |
| `get(timeout, unit)` | Blocks up to timeout; throws `TimeoutException` |
| `isDone()` | `true` if completed (success, failure, or cancelled) |
| `isCancelled()` | `true` if cancelled before completion |
| `cancel(mayInterruptIfRunning)` | Attempts to cancel; returns `false` if already done |

---

## ExecutorService Methods Reference

### Submission Methods

| Method | Returns | Description |
|---|---|---|
| `execute(Runnable)` | `void` | Submit fire-and-forget task (inherited from Executor) |
| `submit(Runnable)` | `Future<?>` | Submit Runnable, returns Future with null result |
| `submit(Runnable, T result)` | `Future<T>` | Submit Runnable, returns Future with given result on success |
| `submit(Callable<T>)` | `Future<T>` | Submit task that returns a value |
| `invokeAll(Collection<Callable<T>>)` | `List<Future<T>>` | Submit all, block until ALL complete |
| `invokeAll(Collection, timeout, unit)` | `List<Future<T>>` | Submit all, block up to timeout |
| `invokeAny(Collection<Callable<T>>)` | `T` | Submit all, return first completed result |
| `invokeAny(Collection, timeout, unit)` | `T` | Same with timeout |

### Lifecycle Methods

| Method | Returns | Description |
|---|---|---|
| `shutdown()` | `void` | Stop accepting new tasks; finish queued tasks |
| `shutdownNow()` | `List<Runnable>` | Attempt to stop all tasks; returns list of pending tasks |
| `isShutdown()` | `boolean` | `true` if `shutdown()` was called |
| `isTerminated()` | `boolean` | `true` if all tasks finished after shutdown |
| `awaitTermination(timeout, unit)` | `boolean` | Block until terminated or timeout; returns `true` if terminated |

---

## ScheduledExecutorService Methods Reference

`ScheduledExecutorService` extends `ExecutorService` with time-based scheduling.

| Method | Returns | Description |
|---|---|---|
| `schedule(Runnable, delay, unit)` | `ScheduledFuture<?>` | Run once after a delay |
| `schedule(Callable<V>, delay, unit)` | `ScheduledFuture<V>` | Run once after delay, return value |
| `scheduleAtFixedRate(Runnable, initialDelay, period, unit)` | `ScheduledFuture<?>` | Run repeatedly; period measured from **start** of last run |
| `scheduleWithFixedDelay(Runnable, initialDelay, delay, unit)` | `ScheduledFuture<?>` | Run repeatedly; delay measured from **end** of last run |

### `scheduleAtFixedRate` vs `scheduleWithFixedDelay`

```
scheduleAtFixedRate(task, 0, 2s):
  |--task(1s)--|  wait 1s  |--task(1s)--|  wait 1s  |...
  ← period=2s from start →

scheduleWithFixedDelay(task, 0, 2s):
  |--task(1s)--|  wait 2s  |--task(1s)--|  wait 2s  |...
               ← delay=2s from end →
```

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

// Once after 3 seconds
scheduler.schedule(() -> System.out.println("One-time"), 3, TimeUnit.SECONDS);

// Every 2 seconds from start time
ScheduledFuture<?> rate = scheduler.scheduleAtFixedRate(
    () -> System.out.println("Fixed rate"), 0, 2, TimeUnit.SECONDS);

// 2 seconds after each completion
ScheduledFuture<?> delay = scheduler.scheduleWithFixedDelay(
    () -> System.out.println("Fixed delay"), 0, 2, TimeUnit.SECONDS);

// Cancel when done
rate.cancel(false);
delay.cancel(false);
scheduler.shutdown();
```

---

## Lifecycle Management

Always shut down an `ExecutorService` — failing to do so leaks threads and prevents JVM exit.

```
Running → shutdown() → Shutdown (no new tasks, existing tasks finish)
        → awaitTermination() timeout → shutdownNow() → Terminated
```

### Standard graceful shutdown pattern

```java
ExecutorService executor = Executors.newFixedThreadPool(4);

// ... submit tasks ...

executor.shutdown();                            // stop accepting new tasks
try {
    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();                 // force stop if timeout
        if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
            System.err.println("Executor did not terminate");
        }
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

### Java 19+ try-with-resources

```java
try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
    executor.submit(() -> System.out.println("task"));
} // auto-shutdown on exit
```

---

## Executor vs ExecutorService vs ScheduledExecutorService

| Feature | `Executor` | `ExecutorService` | `ScheduledExecutorService` |
|---|---|---|---|
| Submit `Runnable` | ✅ | ✅ | ✅ |
| Submit `Callable` | ❌ | ✅ | ✅ |
| Returns `Future` | ❌ | ✅ | ✅ |
| `shutdown()` / lifecycle | ❌ | ✅ | ✅ |
| `invokeAll` / `invokeAny` | ❌ | ✅ | ✅ |
| Delayed execution | ❌ | ❌ | ✅ |
| Periodic execution | ❌ | ❌ | ✅ |

---

## Thread Pool Types Comparison

| Pool Type | Threads | Queue | Best For | Avoid When |
|---|---|---|---|---|
| `FixedThreadPool(n)` | Fixed N | Unbounded `LinkedBlockingQueue` | Stable, predictable load | Memory-sensitive apps (unbounded queue) |
| `SingleThreadExecutor` | 1 | Unbounded `LinkedBlockingQueue` | Sequential ordered tasks | Parallel workloads |
| `CachedThreadPool` | 0 to ∞ | `SynchronousQueue` (no buffer) | Short-lived bursty tasks | Long-running tasks |
| `ScheduledThreadPool(n)` | Fixed N | `DelayedWorkQueue` | Delayed / periodic tasks | High-throughput tasks |
| `VirtualThreadPerTask` *(Java 21+)* | 1 per task (virtual) | None | I/O-bound high concurrency | CPU-bound tasks |

---

## Real-World Use Cases

| Use Case | Recommended Pool |
|---|---|
| Web server handling HTTP requests | `FixedThreadPool` |
| Background file processing | `FixedThreadPool` |
| Database connection pool tasks | `FixedThreadPool` |
| Sequential audit/event log writes | `SingleThreadExecutor` |
| Short-lived async callbacks | `CachedThreadPool` |
| Scheduled reports / cron jobs | `ScheduledThreadPool` |
| Retry logic with delay | `ScheduledThreadPool` |
| Millions of I/O-bound requests | `VirtualThreadPerTask` *(Java 21+)* |
| Parallel batch data processing | `ForkJoinPool` |

---

## Best Practices

- ✅ Always prefer `ExecutorService` over raw `new Thread()`
- ✅ Always call `shutdown()` — never leave a pool running indefinitely
- ✅ Always use `awaitTermination()` + `shutdownNow()` for graceful shutdown
- ✅ Use `submit()` over `execute()` — exceptions are captured in `Future`
- ✅ Always call `future.get()` in a try-catch for `ExecutionException`
- ✅ Use `FixedThreadPool` for stable loads, `CachedThreadPool` for bursty short tasks
- ✅ Avoid `newCachedThreadPool()` for long-running tasks — unbounded thread creation
- ✅ Prefer `ScheduledExecutorService` over `Timer` — safer, supports multiple threads
- ✅ Use `invokeAll()` to run batch tasks and wait for all results at once
- ✅ Use `invokeAny()` for redundant parallel calls — take the fastest result
- ✅ Provide a custom `ThreadPoolExecutor` in production for full control
- ✅ Use `try-with-resources` on `ExecutorService` (Java 19+ `AutoCloseable`)

---

## Examples in this folder

| File | Demonstrates |
|---|---|
| [`BasicExecutorExample.java`](BasicExecutorExample.java) | `Executor` interface with single and fixed pool |
| [`FixedThreadPoolExample.java`](FixedThreadPoolExample.java) | Fixed pool of 3 threads handling 9 tasks with queuing |
| [`SingleThreadExecutorExample.java`](SingleThreadExecutorExample.java) | Sequential task execution in guaranteed order |
| [`CachedThreadPoolExample.java`](CachedThreadPoolExample.java) | Dynamic thread creation + idle thread reuse |
| [`CallableFutureExample.java`](CallableFutureExample.java) | `Callable` + `Future` — return values, timeout, exceptions |
| [`InvokeAllInvokeAnyExample.java`](InvokeAllInvokeAnyExample.java) | `invokeAll` (wait all), `invokeAny` (first wins), mixed failures |
| [`ScheduledExecutorExample.java`](ScheduledExecutorExample.java) | One-time delay, fixed-rate, fixed-delay periodic tasks |
| [`GracefulShutdownExample.java`](GracefulShutdownExample.java) | `shutdown` → `awaitTermination` → `shutdownNow` pattern |

---

## 🛠️ Requirements

- Java 5+ — `Executor`, `ExecutorService`, `ScheduledExecutorService`, `Callable`, `Future`
- Java 8+ — lambda expressions
- Java 19+ — `ExecutorService` implements `AutoCloseable` (try-with-resources)
- Java 21+ — `Executors.newVirtualThreadPerTaskExecutor()`

---

## 📄 License

This project is open-source and available under the [MIT License](../../LICENSE).
