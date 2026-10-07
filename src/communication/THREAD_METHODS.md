# 🔔 Thread Coordination Methods in Java

A complete reference for the five core thread coordination methods in Java — `wait()`, `notify()`, `notifyAll()`, `join()`, and `yield()` — with purpose, rules, examples, and comparisons.

---

## 📋 Table of Contents

- [Overview](#overview)
- [wait()](#wait)
- [notify()](#notify)
- [notifyAll()](#notifyall)
- [join()](#join)
- [yield()](#yield)
- [Side-by-Side Comparison](#side-by-side-comparison)
- [How They Work Together](#how-they-work-together)
- [Thread State Transitions](#thread-state-transitions)
- [Quick Rules](#quick-rules)

---

## Overview

| Method | Defined In | Thread State After | Releases Lock | Needs `synchronized` |
|---|---|---|---|---|
| `wait()` | `Object` | WAITING / TIMED_WAITING | ✅ Yes | ✅ Yes |
| `notify()` | `Object` | Caller stays RUNNABLE | ❌ No | ✅ Yes |
| `notifyAll()` | `Object` | Caller stays RUNNABLE | ❌ No | ✅ Yes |
| `join()` | `Thread` | WAITING / TIMED_WAITING | ❌ No | ❌ No |
| `yield()` | `Thread` | RUNNABLE | ❌ No | ❌ No |

---

## wait()

### What is it?

`wait()` causes the **current thread to release its lock** on the object and enter the **WAITING** state until another thread calls `notify()` or `notifyAll()` on the same object.

### Key Rules
- ✅ Must be called **inside a `synchronized` block or method**
- ✅ Always call `wait()` inside a **`while` loop** — never `if` — to guard against spurious wakeups
- ✅ Automatically **releases the monitor lock** while waiting
- ✅ Re-acquires the lock before returning

### Syntax

```java
synchronized (lock) {
    while (!conditionMet) {       // ← while, not if!
        lock.wait();              // releases lock, enters WAITING state
    }
    // condition is now true — safe to proceed
}
```

### Variants

| Method | Behaviour |
|---|---|
| `wait()` | Waits indefinitely until `notify()` / `notifyAll()` is called |
| `wait(long millis)` | Waits at most N milliseconds, then auto-wakes |
| `wait(long millis, int nanos)` | Waits with nanosecond precision |

### What happens step-by-step

```
1. Thread calls wait() inside synchronized block
2. Thread RELEASES the monitor lock
3. Thread enters WAITING state (sleeps)
4. Another thread calls notify() or notifyAll()
5. Thread wakes up, COMPETES to re-acquire the lock
6. Thread re-acquires lock, re-checks the while condition
7. Thread proceeds if condition is true
```

### Spurious Wakeup — Why `while`, not `if`?

```java
// ❌ WRONG — if condition not met after wakeup, thread proceeds incorrectly
synchronized (lock) {
    if (!dataReady) {
        lock.wait();          // spurious wakeup — wakes even without notify
    }
    process(); // may run when data is NOT ready!
}

// ✅ CORRECT — while re-checks condition after every wakeup
synchronized (lock) {
    while (!dataReady) {
        lock.wait();          // keeps waiting until condition is genuinely true
    }
    process(); // guaranteed: data IS ready
}
```

### Common Mistake — Calling `wait()` without `synchronized`

```java
// ❌ Throws IllegalMonitorStateException
lock.wait(); // must own the lock first!

// ✅ Correct
synchronized (lock) {
    lock.wait();
}
```

---

## notify()

### What is it?

`notify()` wakes up **ONE** thread that is currently waiting on the same object's monitor. The woken thread must re-acquire the lock before it can continue.

### Key Rules
- ✅ Must be called **inside `synchronized`**
- ⚠️ Wakes **one random** waiting thread — you cannot choose which one
- ✅ The calling thread **keeps the lock** until it exits the `synchronized` block
- ⚠️ If the woken thread's condition is still false, it will `wait()` again — other threads may be permanently missed

### Syntax

```java
synchronized (lock) {
    condition = true;
    lock.notify();   // wake up ONE random waiting thread
}
```

### When to use `notify()`

- ✅ Only when **one thread** is waiting on the lock
- ✅ All waiting threads have **identical conditions** (any one can proceed)
- ❌ Avoid when multiple threads wait with **different conditions** — use `notifyAll()` instead

### Risk — Missed Signal

```
Thread-A waits on condition X
Thread-B waits on condition Y
Thread-C calls notify() intending to wake Thread-A

If Thread-B is woken instead:
  Thread-B checks condition Y → false → calls wait() again
  Thread-A is still sleeping → condition X never triggered again
  → Thread-A starves forever
```

---

## notifyAll()

### What is it?

`notifyAll()` wakes up **ALL threads** currently waiting on the same object's monitor. All woken threads compete to re-acquire the lock one at a time — each re-checks its `while` condition and either proceeds or calls `wait()` again.

### Key Rules
- ✅ Must be called **inside `synchronized`**
- ✅ Safer than `notify()` — no risk of waking the wrong thread
- ✅ All threads re-check their condition — only those whose condition is true proceed
- ⚠️ Slightly more overhead than `notify()` — all waiting threads wake and compete

### Syntax

```java
synchronized (lock) {
    condition = true;
    lock.notifyAll();   // wake ALL waiting threads
}
```

### `notify()` vs `notifyAll()`

| | `notify()` | `notifyAll()` |
|---|---|---|
| Threads woken | 1 (random) | All waiting |
| Safety | Can miss correct thread | Always wakes correct thread |
| Performance | Slightly faster | More competition overhead |
| Risk | Missed signal / starvation | None |
| Use when | Single waiter or identical conditions | Multiple waiters or different conditions |

> ✅ **Best practice:** Default to `notifyAll()`. Only use `notify()` when you are certain exactly one thread is waiting.

---

## join()

### What is it?

`thread.join()` makes the **calling thread wait** until the target thread **terminates**. Used to enforce sequencing — one thread must finish before another proceeds.

### Key Rules
- ✅ No `synchronized` required
- ✅ Does **not** release any locks the calling thread holds
- ✅ Throws `InterruptedException` if the waiting thread is interrupted

### Syntax

```java
Thread worker = new Thread(() -> {
    System.out.println("Working...");
});

worker.start();
worker.join();   // calling thread blocks here until worker finishes
System.out.println("Worker done. Now main continues.");
```

### Variants

| Method | Behaviour |
|---|---|
| `join()` | Waits indefinitely until thread terminates |
| `join(long millis)` | Waits at most N milliseconds |
| `join(long millis, int nanos)` | Waits with nanosecond precision |

### Chained sequential execution

```java
Thread step1 = new Thread(() -> System.out.println("Step 1"));
Thread step2 = new Thread(() -> System.out.println("Step 2"));
Thread step3 = new Thread(() -> System.out.println("Step 3"));

step1.start();
step1.join();    // step2 only starts after step1 completes

step2.start();
step2.join();    // step3 only starts after step2 completes

step3.start();
step3.join();
```

### `join()` with timeout

```java
thread.start();
thread.join(2000);   // wait max 2 seconds

if (thread.isAlive()) {
    System.out.println("Thread still running after timeout!");
    thread.interrupt(); // optionally cancel it
}
```

### `wait()` vs `join()`

| | `wait()` | `join()` |
|---|---|---|
| Defined in | `Object` | `Thread` |
| Releases lock | ✅ Yes | ❌ No |
| Woken by | `notify()` / `notifyAll()` / timeout | Thread termination / timeout |
| Needs `synchronized` | ✅ Yes | ❌ No |
| Used for | Condition-based coordination | Waiting for thread to finish |
| General purpose | ✅ Any object monitor | ❌ Thread lifecycle only |

---

## yield()

### What is it?

`Thread.yield()` is a **hint** to the thread scheduler that the current thread is willing to **give up its current CPU time slice** so other threads of equal or higher priority can run.

### Key Rules
- ⚠️ **Not guaranteed** — the scheduler may ignore this hint entirely
- ✅ Thread stays in **RUNNABLE** state — it does NOT block or sleep
- ✅ Does **not** release any lock
- ⚠️ Rarely needed in modern Java — prefer higher-level tools

### Syntax

```java
while (!conditionMet) {
    Thread.yield();   // hint: let other threads run while I spin-wait
}
```

### When to use `yield()`

| Use case | Better alternative |
|---|---|
| Busy-wait loop | `wait()` / `BlockingQueue` / `LockSupport.park()` |
| CPU back-off | `Thread.sleep(1)` |
| Cooperative multitasking | `ExecutorService` with thread pool |
| Testing/debugging thread ordering | `Thread.sleep(ms)` |

> ✅ In production code, `yield()` is almost never the right choice. Use `wait()`, `sleep()`, or `BlockingQueue` instead.

### `sleep()` vs `yield()`

| | `sleep(ms)` | `yield()` |
|---|---|---|
| Blocks for | Fixed time | No fixed time |
| Guaranteed pause | ✅ Yes | ❌ No |
| Thread state | TIMED_WAITING | RUNNABLE |
| Releases lock | ❌ No | ❌ No |
| Use for | Timed pauses | Cooperative hint only |

---

## Side-by-Side Comparison

| | `wait()` | `notify()` | `notifyAll()` | `join()` | `yield()` |
|---|---|---|---|---|---|
| **Defined in** | `Object` | `Object` | `Object` | `Thread` | `Thread` |
| **Thread state after** | WAITING | Caller: RUNNABLE | Caller: RUNNABLE | WAITING | RUNNABLE |
| **Releases lock** | ✅ Yes | ❌ No | ❌ No | ❌ No | ❌ No |
| **Needs `synchronized`** | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| **Woken by** | `notify` / timeout | — | — | Thread terminates | Scheduler |
| **Guaranteed** | ✅ Reliable | ⚠️ Random | ✅ All wake | ✅ Reliable | ❌ Hint only |
| **Purpose** | Pause until condition | Wake one waiter | Wake all waiters | Wait for thread end | Give up CPU turn |

---

## How They Work Together

The classic **Producer-Consumer** pattern uses `wait()` and `notifyAll()` together:

```java
private final Queue<Integer> buffer = new LinkedList<>();
private final int CAPACITY = 5;
private final Object lock = new Object();

// Producer
public void produce(int value) throws InterruptedException {
    synchronized (lock) {
        while (buffer.size() == CAPACITY) {
            lock.wait();              // buffer full — wait for consumer
        }
        buffer.offer(value);
        System.out.println("Produced: " + value);
        lock.notifyAll();             // wake consumer(s)
    }
}

// Consumer
public int consume() throws InterruptedException {
    synchronized (lock) {
        while (buffer.isEmpty()) {
            lock.wait();              // buffer empty — wait for producer
        }
        int value = buffer.poll();
        System.out.println("Consumed: " + value);
        lock.notifyAll();             // wake producer(s)
        return value;
    }
}
```

### Coordination with `join()`

```java
// Fetch data in parallel, then process once all are done
Thread t1 = new Thread(() -> fetchFromServiceA());
Thread t2 = new Thread(() -> fetchFromServiceB());

t1.start();
t2.start();

t1.join();   // wait for both to finish
t2.join();

processResults(); // runs only after both threads complete
```

---

## Thread State Transitions

```
                    start()
   NEW  ──────────────────────────►  RUNNABLE
                                         │
                            CPU scheduled│
                                         ▼
                                      RUNNING
                                    ╱    │    ╲
                 wait() / join()   ╱     │     ╲  yield()
                                  ╱      │      ╲
                                 ▼       │       ▼
                              WAITING    │    RUNNABLE
                                 │       │    (back in queue)
          notify() /             │       │
          notifyAll() /          │       │  sleep(ms) / wait(ms) / join(ms)
          thread terminates      │       ▼
                                 │   TIMED_WAITING
                                 │       │
                                 │       │ timeout / notify
                                 ▼       ▼
                              RUNNABLE ◄─┘
                                  │
                          run() ends│
                                  ▼
                              TERMINATED
```

---

## Quick Rules

| Rule | Detail |
|---|---|
| `wait()` → always in `while` loop | Guards against spurious wakeups |
| `wait()` → always in `synchronized` | Throws `IllegalMonitorStateException` otherwise |
| `notify()` → use for single waiter | Risk of missing the right thread with multiple waiters |
| `notifyAll()` → safer default | All waiting threads re-check their condition |
| `join()` → no `synchronized` needed | Just call `thread.join()` directly |
| `join(millis)` → always check `isAlive()` after | Timeout may expire before thread finishes |
| `yield()` → rarely needed | Prefer `wait()`, `sleep()`, `BlockingQueue` in real code |
| Never call `wait()`/`notify()` on `String` or `Integer` literals | Shared pool causes unrelated threads to interfere |

---

## 🛠️ Requirements

- Java 1.0+ — `wait()`, `notify()`, `notifyAll()`, `join()`, `yield()`

---

## 📄 License

This project is open-source and available under the [MIT License](../../LICENSE).
