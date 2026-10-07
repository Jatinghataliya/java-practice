# ⚡ CompletableFuture in Java

A complete reference for `CompletableFuture` — Java's powerful async programming API introduced in **Java 8**.

---

## 📋 Table of Contents

- [What is CompletableFuture?](#what-is-completablefuture)
- [Why use CompletableFuture?](#why-use-completablefuture)
- [Future vs CompletableFuture](#future-vs-completablefuture)
- [Creating a CompletableFuture](#creating-a-completablefuture)
- [Chaining Methods](#chaining-methods)
- [Combining Methods](#combining-methods)
- [Exception Handling Methods](#exception-handling-methods)
- [Completion & Status Methods](#completion--status-methods)
- [All Methods Reference](#all-methods-reference)
- [Real-World Use Cases](#real-world-use-cases)
- [Best Practices](#best-practices)
- [Examples in this folder](#examples-in-this-folder)

---

## What is CompletableFuture?

`CompletableFuture<T>` is a class in `java.util.concurrent` that represents an **asynchronous computation** whose result will be available in the future.

It implements both:
- `Future<T>` — represents a future result
- `CompletionStage<T>` — supports chaining, combining, and composing async operations

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // runs asynchronously on ForkJoinPool.commonPool()
    return "Hello, World!";
});

String result = future.get(); // blocks until result is ready
```

---

## Why use CompletableFuture?

### Problems with plain `Future`:
- `future.get()` **always blocks** — no way to attach a callback
- Cannot chain tasks — each step requires explicit blocking
- No built-in exception handling
- Cannot combine multiple futures easily
- Cannot manually complete a future

### CompletableFuture solves all of this:

| Problem with Future | How CompletableFuture solves it |
|---|---|
| Blocking `get()` | Non-blocking callbacks via `thenApply`, `thenAccept` |
| No chaining | Fluent pipeline with `thenApply → thenApply → thenAccept` |
| No exception handling | `exceptionally()`, `handle()`, `whenComplete()` |
| Cannot combine | `thenCombine()`, `allOf()`, `anyOf()`, `thenCompose()` |
| Cannot manually complete | `complete()`, `completeExceptionally()` |

### Real benefits:
- ✅ **Non-blocking** — thread isn't wasted waiting
- ✅ **Composable** — chain multiple async tasks like a pipeline
- ✅ **Parallel** — run independent tasks simultaneously
- ✅ **Error-resilient** — handle failures gracefully at any stage
- ✅ **Readable** — fluent API replaces nested callbacks

---

## Future vs CompletableFuture

```
Future<T>                          CompletableFuture<T>
─────────────────────────────      ──────────────────────────────────────
get() → blocks                     thenApply() → non-blocking callback
No chaining                        Full fluent pipeline support
No exception handling              exceptionally / handle / whenComplete
No combining                       thenCombine / allOf / anyOf
Cannot manually complete           complete() / completeExceptionally()
Only Callable input                Runnable + Supplier + manual
```

---

## Creating a CompletableFuture

### `supplyAsync` — async task that returns a value

```java
// Uses ForkJoinPool.commonPool() by default
CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> "result");

// With a custom Executor
ExecutorService pool = Executors.newFixedThreadPool(4);
CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> "result", pool);
```

### `runAsync` — async task with no return value

```java
CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
    System.out.println("Fire and forget");
});
```

### `completedFuture` — already-completed future

```java
CompletableFuture<Integer> cf = CompletableFuture.completedFuture(42);
// isDone() = true immediately
```

### Manual creation

```java
CompletableFuture<String> cf = new CompletableFuture<>();
cf.complete("manual result");         // manually set result
cf.completeExceptionally(new RuntimeException("failed")); // manually fail
```

---

## Chaining Methods

Chaining runs each step after the previous one completes, forming a **non-blocking async pipeline**.

---

### `thenApply(Function<T, U>)`
Transforms the result. Like `map()` on a stream. Returns `CompletableFuture<U>`.

```java
CompletableFuture.supplyAsync(() -> "hello")
    .thenApply(s -> s.toUpperCase())   // "HELLO"
    .thenApply(s -> "Result: " + s);   // "Result: HELLO"
```

---

### `thenAccept(Consumer<T>)`
Consumes the result — no return value. Returns `CompletableFuture<Void>`.

```java
CompletableFuture.supplyAsync(() -> 42)
    .thenAccept(value -> System.out.println("Got: " + value));
```

---

### `thenRun(Runnable)`
Runs after completion — ignores the result entirely. Returns `CompletableFuture<Void>`.

```java
CompletableFuture.supplyAsync(() -> "data")
    .thenRun(() -> System.out.println("Task complete. Sending notification."));
```

---

### `thenApplyAsync` / `thenAcceptAsync` / `thenRunAsync`
Same as above, but **forces the next step to run on a different thread** (async continuation).

```java
CompletableFuture.supplyAsync(() -> "data")
    .thenApplyAsync(s -> s.toUpperCase())   // next step on ForkJoinPool thread
    .thenAcceptAsync(System.out::println);  // another thread
```

---

## Combining Methods

### `thenCombine(CompletableFuture<U>, BiFunction<T, U, V>)`
Combines results of **two independent futures** once both complete.

```java
CompletableFuture<Integer> price    = CompletableFuture.supplyAsync(() -> 1200);
CompletableFuture<Integer> discount = CompletableFuture.supplyAsync(() -> 200);

CompletableFuture<Integer> total = price.thenCombine(discount,
    (p, d) -> p - d); // 1000
```

---

### `thenCompose(Function<T, CompletableFuture<U>>)`
Chains futures where the **second depends on the first's result** (like `flatMap`).
Avoids `CompletableFuture<CompletableFuture<T>>` nesting.

```java
CompletableFuture.supplyAsync(() -> "user-42")          // fetch userId
    .thenCompose(userId -> fetchProfile(userId))        // use userId → fetch profile
    .thenCompose(profile -> fetchSettings(profile));    // use profile → fetch settings
```

> **`thenApply` vs `thenCompose`**
> - `thenApply`   → `T → U`                       (sync transform)
> - `thenCompose` → `T → CompletableFuture<U>`    (async chaining)

---

### `allOf(CompletableFuture<?>...)`
Waits for **ALL** futures to complete. Returns `CompletableFuture<Void>`.

```java
CompletableFuture<String> t1 = CompletableFuture.supplyAsync(() -> "A");
CompletableFuture<String> t2 = CompletableFuture.supplyAsync(() -> "B");
CompletableFuture<String> t3 = CompletableFuture.supplyAsync(() -> "C");

CompletableFuture.allOf(t1, t2, t3)
    .thenRun(() -> System.out.println("All done: "
        + t1.join() + t2.join() + t3.join()))
    .get();
```

> ⚠️ `allOf` returns `Void` — you must call `.join()` / `.get()` on individual futures to collect results.

---

### `anyOf(CompletableFuture<?>...)`
Returns the result of the **FIRST** future to complete. Returns `CompletableFuture<Object>`.

```java
CompletableFuture<Object> fastest = CompletableFuture.anyOf(
    CompletableFuture.supplyAsync(() -> { sleep(500); return "Slow"; }),
    CompletableFuture.supplyAsync(() -> { sleep(100); return "Fast"; })
);

System.out.println(fastest.get()); // "Fast"
```

---

## Exception Handling Methods

### `exceptionally(Function<Throwable, T>)`
Catches exception and provides a **fallback value**. Only called if an exception occurred.

```java
CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("Error!");
})
.exceptionally(ex -> "fallback value");
```

---

### `handle(BiFunction<T, Throwable, U>)`
**Always called** — whether success or failure. Receives `(result, exception)` — one will be null.
Can transform both the result and the exception.

```java
CompletableFuture.supplyAsync(() -> riskyOperation())
    .handle((result, ex) -> {
        if (ex != null) return "RECOVERED: " + ex.getMessage();
        return result.toUpperCase();
    });
```

---

### `whenComplete(BiConsumer<T, Throwable>)`
**Always called** — for side-effects only (logging, cleanup). **Cannot transform** the result.
Exception still propagates after this.

```java
CompletableFuture.supplyAsync(() -> compute())
    .whenComplete((result, ex) -> {
        if (ex != null) log("Error: " + ex.getMessage());
        else            log("Success: " + result);
    });
```

---

### Comparison: `exceptionally` vs `handle` vs `whenComplete`

| Feature | `exceptionally` | `handle` | `whenComplete` |
|---|---|---|---|
| Called on success | ❌ | ✅ | ✅ |
| Called on failure | ✅ | ✅ | ✅ |
| Can transform result | ✅ | ✅ | ❌ |
| Exception propagates | No (swallowed) | No (swallowed) | Yes |
| Use for | Fallback value | Result transformation | Logging / cleanup |

---

## Completion & Status Methods

### Getting the result

| Method | Description |
|---|---|
| `get()` | Blocks; throws `ExecutionException`, `InterruptedException` |
| `get(timeout, unit)` | Blocks up to timeout; throws `TimeoutException` |
| `join()` | Blocks; throws unchecked `CompletionException` (use in streams) |
| `getNow(fallback)` | Returns fallback immediately if result isn't ready yet |

### Checking status

| Method | Description |
|---|---|
| `isDone()` | `true` if completed (success, failure, or cancelled) |
| `isCompletedExceptionally()` | `true` if completed with an exception |
| `isCancelled()` | `true` if cancelled |

### Manual completion

| Method | Description |
|---|---|
| `complete(T value)` | Manually complete with a value |
| `completeExceptionally(Throwable)` | Manually complete with a failure |
| `cancel(mayInterruptIfRunning)` | Cancel the computation |
| `obtrudeValue(T value)` | Force-overwrite the result (even if already completed) |
| `obtrudeException(Throwable)` | Force-overwrite with an exception |

---

## All Methods Reference

### Factory / Creation

| Method | Returns | Description |
|---|---|---|
| `supplyAsync(Supplier)` | `CF<T>` | Async task returning a value |
| `supplyAsync(Supplier, Executor)` | `CF<T>` | Same, on custom Executor |
| `runAsync(Runnable)` | `CF<Void>` | Async task, no return value |
| `runAsync(Runnable, Executor)` | `CF<Void>` | Same, on custom Executor |
| `completedFuture(T)` | `CF<T>` | Already-completed future |
| `failedFuture(Throwable)` *(Java 9+)* | `CF<T>` | Already-failed future |
| `new CompletableFuture<>()` | `CF<T>` | Empty future for manual completion |

### Chaining

| Method | Returns | Description |
|---|---|---|
| `thenApply(Function)` | `CF<U>` | Transform result (same thread) |
| `thenApplyAsync(Function)` | `CF<U>` | Transform result (async thread) |
| `thenAccept(Consumer)` | `CF<Void>` | Consume result, no return |
| `thenAcceptAsync(Consumer)` | `CF<Void>` | Consume result async |
| `thenRun(Runnable)` | `CF<Void>` | Run after, ignore result |
| `thenRunAsync(Runnable)` | `CF<Void>` | Run after async, ignore result |

### Combining

| Method | Returns | Description |
|---|---|---|
| `thenCombine(CF, BiFunction)` | `CF<V>` | Combine two independent futures |
| `thenCombineAsync(CF, BiFunction)` | `CF<V>` | Same, async |
| `thenCompose(Function)` | `CF<U>` | Chain dependent futures (flatMap) |
| `thenComposeAsync(Function)` | `CF<U>` | Same, async |
| `allOf(CF...)` | `CF<Void>` | Wait for all to complete |
| `anyOf(CF...)` | `CF<Object>` | Wait for first to complete |
| `thenAcceptBoth(CF, BiConsumer)` | `CF<Void>` | Consume both results, no return |
| `runAfterBoth(CF, Runnable)` | `CF<Void>` | Run after both complete |
| `runAfterEither(CF, Runnable)` | `CF<Void>` | Run after either completes |
| `acceptEither(CF, Consumer)` | `CF<Void>` | Consume first to complete |
| `applyToEither(CF, Function)` | `CF<U>` | Transform first to complete |

### Exception Handling

| Method | Returns | Description |
|---|---|---|
| `exceptionally(Function)` | `CF<T>` | Fallback on failure only |
| `exceptionallyAsync(Function)` *(Java 12+)* | `CF<T>` | Async fallback on failure |
| `handle(BiFunction)` | `CF<U>` | Always called, can transform |
| `handleAsync(BiFunction)` | `CF<U>` | Always called, async |
| `whenComplete(BiConsumer)` | `CF<T>` | Always called, side-effect only |
| `whenCompleteAsync(BiConsumer)` | `CF<T>` | Same, async |

### Completion & Control

| Method | Returns | Description |
|---|---|---|
| `get()` | `T` | Block for result (checked exceptions) |
| `get(timeout, unit)` | `T` | Block with timeout |
| `join()` | `T` | Block (unchecked exception) |
| `getNow(fallback)` | `T` | Return now or fallback |
| `complete(T)` | `boolean` | Manually set result |
| `completeExceptionally(Throwable)` | `boolean` | Manually set failure |
| `cancel(boolean)` | `boolean` | Cancel computation |
| `isDone()` | `boolean` | Is completed (any way) |
| `isCompletedExceptionally()` | `boolean` | Completed with error |
| `isCancelled()` | `boolean` | Was cancelled |
| `obtrudeValue(T)` | `void` | Force-overwrite result |
| `obtrudeException(Throwable)` | `void` | Force-overwrite exception |
| `getNumberOfDependents()` | `int` | Count of dependent stages |
| `minimalCompletionStage()` *(Java 9+)* | `CS<T>` | Read-only view |
| `copy()` *(Java 9+)* | `CF<T>` | New future that mirrors this one |
| `toCompletableFuture()` | `CF<T>` | Returns itself |

---

## Real-World Use Cases

| Use Case | Pattern |
|---|---|
| Parallel API calls (independent) | `supplyAsync` × N + `allOf` |
| Sequential dependent API calls | `thenCompose` chaining |
| Combine two service results | `thenCombine` |
| First response wins (redundant calls) | `anyOf` |
| Async with fallback on failure | `exceptionally` |
| Logging without altering result | `whenComplete` |
| Retry / transform on error | `handle` |
| Fire-and-forget side effects | `thenRun` / `thenAccept` |
| Manual future (test mocks, adapters) | `new CompletableFuture<>()` + `complete()` |

---

## Best Practices

- ✅ Always handle exceptions — unhandled failures are silently swallowed
- ✅ Use `join()` inside streams instead of `get()` (avoids checked exceptions)
- ✅ Use `thenCompose` for dependent async steps — not `thenApply` returning a future
- ✅ Use `thenCombine` for **independent** parallel tasks (not `thenCompose`)
- ✅ Provide a custom `Executor` for CPU-intensive tasks — don't saturate `ForkJoinPool.commonPool()`
- ✅ Use `allOf` + stream + `join()` to collect all results as a `List`
- ✅ Prefer `handle()` over `exceptionally()` when you need access to both result and exception
- ✅ Use `whenComplete` for logging and cleanup only — it does not transform results
- ✅ Avoid blocking `get()` inside another `CompletableFuture` — it defeats the purpose
- ✅ Use `completeExceptionally()` in manual futures to ensure errors propagate correctly

---

## Examples in this folder

| File | Demonstrates |
|---|---|
| [`BasicCompletableFutureExample.java`](BasicCompletableFutureExample.java) | `supplyAsync`, `runAsync`, `completedFuture`, `complete()`, `getNow()` |
| [`ChainingExample.java`](ChainingExample.java) | `thenApply`, `thenAccept`, `thenRun`, `thenApplyAsync` |
| [`CombiningExample.java`](CombiningExample.java) | `thenCombine`, `thenCompose`, `allOf`, `anyOf` |
| [`ExceptionHandlingExample.java`](ExceptionHandlingExample.java) | `exceptionally`, `handle`, `whenComplete`, `completeExceptionally` |
| [`AsyncPipelineExample.java`](AsyncPipelineExample.java) | Real-world e-commerce order pipeline + parallel multi-order processing |

---

## 🛠️ Requirements

- Java 8+ — `CompletableFuture`, all core methods
- Java 9+ — `failedFuture()`, `copy()`, `minimalCompletionStage()`
- Java 12+ — `exceptionallyAsync()`

---

## 📄 License

This project is open-source and available under the [MIT License](../../LICENSE).
