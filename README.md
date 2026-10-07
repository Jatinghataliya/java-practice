# 🧵 Java Threads

A comprehensive guide and reference for understanding **Multithreading in Java** — covering core concepts, thread lifecycle, synchronization, concurrency utilities, and best practices.

---

## 📋 Table of Contents

- [What is a Thread?](#what-is-a-thread)
- [Process vs Thread](#process-vs-thread)
- [Thread Lifecycle](#thread-lifecycle)
- [Creating Threads](#creating-threads)
- [Thread Methods](#thread-methods)
- [Thread Priority](#thread-priority)
- [Synchronization](#synchronization)
- [Inter-Thread Communication](#inter-thread-communication)
- [Concurrency Utilities](#concurrency-utilities)
- [Common Problems](#common-problems)
- [Virtual Threads (Java 21+)](#virtual-threads-java-21)
- [Best Practices](#best-practices)

---

## What is a Thread?

A **thread** is the smallest unit of execution within a Java program. It runs concurrently with other threads inside a single JVM process, sharing the same heap memory while maintaining its own stack, program counter, and local variables.

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Running on: " + Thread.currentThread().getName());
    }
}
```

---

## Process vs Thread

| Feature        | Process                  | Thread                        |
|----------------|--------------------------|-------------------------------|
| Memory         | Separate memory space    | Shares heap with other threads |
| Creation cost  | Heavy                    | Lightweight                   |
| Communication  | Inter-process (IPC)      | Shared memory                 |
| Crash impact   | Isolated                 | Can crash the whole process   |

---

## Thread Lifecycle

A thread passes through the following states:

```
New → Runnable → Running → Blocked / Waiting / Timed Waiting → Terminated
```

| State             | Description                                              |
|-------------------|----------------------------------------------------------|
| `NEW`             | Thread created but `start()` not yet called              |
| `RUNNABLE`        | Ready to run or currently running                        |
| `BLOCKED`         | Waiting to acquire a monitor lock                        |
| `WAITING`         | Waiting indefinitely for another thread (`wait()`, `join()`) |
| `TIMED_WAITING`   | Waiting for a specified time (`sleep()`, `wait(ms)`)     |
| `TERMINATED`      | Execution completed                                      |

---

## Creating Threads

### 1. Extending `Thread` class

```java
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread: " + Thread.currentThread().getName());
    }
}

MyThread t = new MyThread();
t.start();
```

### 2. Implementing `Runnable` *(recommended)*

```java
class MyRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("Runnable Thread: " + Thread.currentThread().getName());
    }
}

Thread t = new Thread(new MyRunnable());
t.start();
```

### 3. Lambda Expression (Java 8+)

```java
Thread t = new Thread(() -> {
    System.out.println("Lambda Thread: " + Thread.currentThread().getName());
});
t.start();
```

### 4. `ExecutorService` *(production-preferred)*

```java
ExecutorService executor = Executors.newFixedThreadPool(4);

executor.submit(() -> System.out.println("Thread from pool"));

executor.shutdown();
```

### 5. `Callable` + `Future` (for return values)

```java
Callable<Integer> task = () -> 42;

ExecutorService executor = Executors.newSingleThreadExecutor();
Future<Integer> future = executor.submit(task);

System.out.println("Result: " + future.get()); // blocks until done
executor.shutdown();
```

---

## Thread Methods

| Method                    | Description                                           |
|---------------------------|-------------------------------------------------------|
| `start()`                 | Starts the thread; internally calls `run()`           |
| `run()`                   | Entry point for thread logic                          |
| `sleep(long ms)`          | Pauses the thread for the given duration              |
| `join()`                  | Waits for this thread to finish before continuing     |
| `interrupt()`             | Interrupts a sleeping or waiting thread               |
| `isAlive()`               | Returns `true` if thread has not yet terminated       |
| `getName()` / `setName()` | Gets or sets the thread name                          |
| `getId()`                 | Returns the thread's unique ID                        |
| `getState()`              | Returns the current `Thread.State`                    |
| `currentThread()`         | Returns a reference to the currently running thread   |
| `yield()`                 | Hints the scheduler to let other threads run          |

---

## Thread Priority

```java
Thread t = new Thread(() -> System.out.println("Priority Thread"));

t.setPriority(Thread.MIN_PRIORITY);  // 1
t.setPriority(Thread.NORM_PRIORITY); // 5 (default)
t.setPriority(Thread.MAX_PRIORITY);  // 10

t.start();
```

> ⚠️ Priority is a **hint** to the OS scheduler — not a strict guarantee of execution order.

---

## Synchronization

When multiple threads access shared mutable data, synchronization prevents race conditions.

### `synchronized` method

```java
class Counter {
    private int count = 0;

    public synchronized void increment() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}
```

### `synchronized` block

```java
class Counter {
    private int count = 0;
    private final Object lock = new Object();

    public void increment() {
        synchronized (lock) {
            count++;
        }
    }
}
```

### `ReentrantLock`

```java
import java.util.concurrent.locks.ReentrantLock;

class Counter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();

    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock(); // always release in finally
        }
    }
}
```

---

## Inter-Thread Communication

Use `wait()`, `notify()`, and `notifyAll()` for coordinating threads sharing a resource.

```java
class SharedResource {
    private boolean ready = false;

    public synchronized void produce() throws InterruptedException {
        ready = true;
        notify(); // wake up waiting thread
    }

    public synchronized void consume() throws InterruptedException {
        while (!ready) {
            wait(); // release lock and wait
        }
        System.out.println("Consumed!");
    }
}
```

---

## Concurrency Utilities

Java's `java.util.concurrent` package provides high-level building blocks:

| Utility                  | Description                                              |
|--------------------------|----------------------------------------------------------|
| `ExecutorService`        | Manages a pool of threads                                |
| `ScheduledExecutorService` | Schedules tasks with delay or fixed rate               |
| `CountDownLatch`         | Waits for N threads to complete before proceeding        |
| `CyclicBarrier`          | Syncs a group of threads at a common barrier point       |
| `Semaphore`              | Controls access to a resource with a permit count        |
| `BlockingQueue`          | Thread-safe queue for producer-consumer scenarios        |
| `CompletableFuture`      | Async programming with chaining and composition          |
| `AtomicInteger`          | Lock-free thread-safe integer operations                 |
| `ConcurrentHashMap`      | Thread-safe high-performance hash map                    |

---

## Common Problems

### 🔴 Race Condition
Two threads read/write shared data without synchronization, producing unpredictable results.

### 🔴 Deadlock
Two threads hold locks and wait for each other's lock — neither can proceed.

```
Thread-1 holds Lock-A, waits for Lock-B
Thread-2 holds Lock-B, waits for Lock-A  ← deadlock
```

### 🔴 Starvation
A low-priority thread never gets CPU time because high-priority threads keep running.

### 🔴 Livelock
Threads keep responding to each other's actions but neither makes any real progress.

---

## Virtual Threads (Java 21+)

Java 21 introduced **Virtual Threads** (Project Loom) — lightweight threads managed by the JVM rather than the OS.

```java
// Create a virtual thread
Thread vt = Thread.ofVirtual().start(() -> {
    System.out.println("Virtual Thread: " + Thread.currentThread());
});

// Using ExecutorService with virtual threads
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> System.out.println("Virtual thread task"));
}
```

| Feature          | Platform Thread   | Virtual Thread       |
|------------------|-------------------|----------------------|
| Managed by       | OS                | JVM                  |
| Memory footprint | ~1 MB stack       | A few KB             |
| Scalability      | Thousands         | Millions             |
| Blocking cost    | High              | Very low             |

---

## Best Practices

- ✅ Prefer `ExecutorService` over raw `Thread` creation
- ✅ Always release locks in a `finally` block
- ✅ Use `AtomicInteger` / `AtomicLong` for simple counters instead of `synchronized`
- ✅ Prefer `ConcurrentHashMap` over `synchronized HashMap`
- ✅ Use `volatile` for simple flags shared between threads
- ✅ Avoid calling `stop()` or `suspend()` — they are deprecated and unsafe
- ✅ Keep synchronized blocks as short as possible
- ✅ Use `ThreadLocal` to give each thread its own isolated variable copy
- ✅ Consider Virtual Threads (Java 21+) for high-concurrency I/O-bound tasks

---

## 🛠️ Requirements

- Java 8+ (for lambdas, streams, `CompletableFuture`)
- Java 21+ (for Virtual Threads)

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
