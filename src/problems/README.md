# ⚠️ Concurrency Problems in Java

A complete reference for the four classic concurrency problems in Java multithreading — **Race Condition**, **Deadlock**, **Starvation**, and **Livelock** — with root causes, visual explanations, and proven fixes.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Race Condition](#race-condition)
- [Deadlock](#deadlock)
- [Starvation](#starvation)
- [Livelock](#livelock)
- [Quick Comparison](#quick-comparison)
- [Examples in this folder](#examples-in-this-folder)

---

## Overview

| Problem | Threads State | Progress | Root Cause |
|---|---|---|---|
| **Race Condition** | Running | ✅ Makes progress | Unsynchronized shared data access |
| **Deadlock** | Blocked | ❌ No progress | Circular lock dependency |
| **Starvation** | Mixed | ⚠️ Some progress, one starved | Unfair resource allocation |
| **Livelock** | Running | ❌ No real progress | Threads react to each other endlessly |

---

## Race Condition

### What is it?

A **Race Condition** occurs when two or more threads read and write shared mutable data **simultaneously** without synchronization, producing **incorrect, unpredictable results** that depend on the order of thread scheduling.

### Root Cause

`count++` looks like one operation but is actually **three steps**:

```
1. READ  the current value of count
2. ADD   1 to it
3. WRITE the new value back
```

Two threads can **interleave** these steps:

```
Thread-1: READ  count = 5
Thread-2: READ  count = 5   ← same value read!
Thread-1: ADD   5 + 1 = 6
Thread-2: ADD   5 + 1 = 6
Thread-1: WRITE count = 6
Thread-2: WRITE count = 6   ← one increment is LOST!
Expected: 7   Actual: 6
```

### Broken Example

```java
class UnsafeCounter {
    private int count = 0;

    public void increment() {
        count++; // NOT atomic — race condition!
    }
}
```

### Fixes

#### Fix 1: `synchronized` method

```java
class SynchronizedCounter {
    private int count = 0;

    public synchronized void increment() {
        count++; // only ONE thread at a time
    }
}
```

#### Fix 2: `AtomicInteger` (lock-free, faster)

```java
class AtomicCounter {
    private final AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        count.incrementAndGet(); // single atomic CAS operation
    }
}
```

#### Fix 3: `ReentrantLock`

```java
class LockedCounter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();

    public void increment() {
        lock.lock();
        try { count++; }
        finally { lock.unlock(); }
    }
}
```

### Fix Comparison

| Fix | Mechanism | Performance | Use When |
|---|---|---|---|
| `synchronized` | Intrinsic monitor lock | Medium | Simple critical sections |
| `AtomicInteger` | Lock-free CAS | Fast | Single-variable counters/flags |
| `ReentrantLock` | Explicit lock | Medium | Need tryLock, fairness, or conditions |

---

## Deadlock

### What is it?

A **Deadlock** occurs when two or more threads are **permanently blocked**, each waiting for a lock held by another — forming a circular dependency that can never be resolved.

### Coffman Conditions (ALL four must hold for deadlock)

| Condition | Description |
|---|---|
| **Mutual Exclusion** | Resources cannot be shared — only one thread at a time |
| **Hold and Wait** | Thread holds one lock while waiting for another |
| **No Preemption** | Locks cannot be forcibly taken from a thread |
| **Circular Wait** | Thread-1 waits for Thread-2, Thread-2 waits for Thread-1 |

### Classic Scenario

```
Thread-1: holds Lock-A → waits for Lock-B
Thread-2: holds Lock-B → waits for Lock-A
                    ↑ DEADLOCK ↑
```

### Broken Example

```java
// Thread-1 acquires Lock-A, then tries Lock-B
synchronized (lockA) {
    synchronized (lockB) { /* work */ }  // waits forever — Thread-2 holds B
}

// Thread-2 acquires Lock-B, then tries Lock-A
synchronized (lockB) {
    synchronized (lockA) { /* work */ }  // waits forever — Thread-1 holds A
}
```

### Fix 1: Lock Ordering — always acquire locks in the SAME order

```java
// BOTH threads acquire Lock-A first, then Lock-B
synchronized (lockA) {       // Thread-1: A first
    synchronized (lockB) {   //           then B
        /* work */
    }
}

synchronized (lockA) {       // Thread-2: also A first
    synchronized (lockB) {   //           then B
        /* work */
    }
}
// No circular wait → no deadlock
```

### Fix 2: `tryLock()` with timeout — back off if lock not available

```java
ReentrantLock lockA = new ReentrantLock();
ReentrantLock lockB = new ReentrantLock();

if (lockA.tryLock(100, TimeUnit.MILLISECONDS)) {
    try {
        if (lockB.tryLock(100, TimeUnit.MILLISECONDS)) {
            try {
                // got both locks — do work
            } finally { lockB.unlock(); }
        }
    } finally { lockA.unlock(); }
} else {
    // back off and retry later
}
```

### Prevention Summary

| Strategy | How it works |
|---|---|
| **Lock Ordering** | All threads acquire locks in the same global order |
| **tryLock + back-off** | Attempt with timeout; release and retry on failure |
| **Single lock** | Use one lock for resources always used together |
| **Lock timeout** | Set max wait time to avoid infinite blocking |
| **Avoid nested locks** | Don't acquire a lock while holding another |

---

## Starvation

### What is it?

**Starvation** occurs when a thread is **perpetually denied** access to a resource because other threads continuously acquire it first — the starved thread is alive but never makes progress.

### Unlike Deadlock

```
Deadlock    → ALL threads blocked (no one progresses)
Starvation  → SOME threads run fine; ONE thread never gets its turn
```

### Common Causes

| Cause | Description |
|---|---|
| **Thread priority** | Low-priority thread starved by high-priority threads |
| **Unfair lock** | Non-fair lock re-grants to recently-released threads |
| **Long critical section** | One thread holds lock so long others time out or give up |
| **Greedy thread** | One thread re-acquires lock immediately after releasing |

### Broken Example

```java
// Non-fair synchronized — no guarantee of FIFO ordering
// Thread-0 holds lock for 80ms; others for 5ms
// Thread-0 re-acquires before others even get a chance
synchronized (lock) {
    doWork(80); // Thread-0 monopolises the lock
}
```

### Fix: Fair `ReentrantLock(true)`

```java
// fair=true → longest-waiting thread gets the lock next (FIFO queue)
ReentrantLock fairLock = new ReentrantLock(true);

fairLock.lock();
try {
    doWork();
} finally {
    fairLock.unlock();
}
```

### Fair vs Unfair Lock

| Property | Unfair Lock (default) | Fair Lock `(true)` |
|---|---|---|
| Order | No guarantee | FIFO — longest waiter first |
| Throughput | Higher | Lower (queuing overhead) |
| Starvation | Possible | Prevented |
| Use when | Max throughput | Fairness required |

### Thread Priority Note

```java
thread.setPriority(Thread.MIN_PRIORITY);  // 1 — least favoured
thread.setPriority(Thread.NORM_PRIORITY); // 5 — default
thread.setPriority(Thread.MAX_PRIORITY);  // 10 — most favoured
```

> ⚠️ Priority is only a **hint** to the OS scheduler — not a guarantee. Avoid relying on it for correctness.

---

## Livelock

### What is it?

A **Livelock** occurs when threads are **actively running** but keep **responding to each other's actions** in a way that prevents any real progress — they're busy but stuck.

### Unlike Deadlock

```
Deadlock → threads are BLOCKED   (waiting, sleeping — no CPU usage)
Livelock → threads are ACTIVE    (running, changing state — using CPU)
           but making NO progress toward completing the task
```

### Classic Analogy

```
Alice ──►   ◄── Bob
(both step RIGHT simultaneously)

Alice  ►    ◄  Bob
(both step LEFT simultaneously)

They keep mirroring each other — both moving, neither gets through.
```

### Broken Example

```java
while (true) {
    if (collision()) {
        stepAside();    // Thread-1 steps right
        sleep(50);
    }
}

while (true) {
    if (collision()) {
        stepAside();    // Thread-2 also steps right — symmetric!
        sleep(50);
    }
}
// Both keep stepping the same way → livelock
```

### Fix 1: Random back-off — break the symmetry

```java
while (!passed) {
    if (blocked()) {
        sleep((long)(Math.random() * 200 + 50)); // random 50–250ms
        stepAside();
    } else {
        proceed(); // different random delays → one clears first
    }
}
```

### Fix 2: Priority ordering — one always yields

```java
// High-priority thread proceeds directly
if (isHighPriority) {
    proceed();
}

// Low-priority thread waits for high-priority to clear
while (highPriorityThreadActive()) {
    sleep(50);
}
proceed();
```

### Fix 3: Retry limit — force through after N attempts

```java
int attempts = 0;
while (!passed && attempts < MAX_ATTEMPTS) {
    if (blocked()) {
        attempts++;
        stepAside();
        sleep(backoff(attempts));
    } else {
        proceed();
    }
}
if (!passed) {
    forceThrough(); // after max retries, proceed unconditionally
}
```

### Fix Comparison

| Fix | How it works | Best for |
|---|---|---|
| **Random back-off** | Different delays break symmetry naturally | Symmetric contention |
| **Priority ordering** | One party always has right of way | Known hierarchy |
| **Retry limit** | Forces progress after N failed attempts | Bounded retry scenarios |

---

## Quick Comparison

| | Race Condition | Deadlock | Starvation | Livelock |
|---|---|---|---|---|
| **Thread state** | Running | Blocked | Mixed | Running |
| **CPU usage** | High | None | Partial | High |
| **Makes progress** | ✅ Wrong results | ❌ None | ⚠️ Partial | ❌ None |
| **Detectable** | Wrong output | JVM thread dump | Slow thread | High CPU, no output |
| **Root cause** | No synchronization | Circular lock wait | Unfair access | Symmetric reaction loop |
| **Primary fix** | `synchronized` / `AtomicInteger` | Lock ordering / `tryLock` | Fair lock | Random back-off |
| **Involves locks** | Maybe | Always | Always | Sometimes |

---

## Examples in this folder

| File | Problem | What it shows |
|---|---|---|
| [`RaceConditionExample.java`](RaceConditionExample.java) | Race Condition | Unsafe counter → wrong count; fixed with `synchronized` and `AtomicInteger` |
| [`DeadlockExample.java`](DeadlockExample.java) | Deadlock | Two threads circular lock wait; fixed with lock ordering + `tryLock` |
| [`StarvationExample.java`](StarvationExample.java) | Starvation | Greedy thread monopolises lock; fixed with `ReentrantLock(true)` + priority demo |
| [`LivelockExample.java`](LivelockExample.java) | Livelock | Hallway analogy — threads mirror each other; fixed with random back-off + priority |

---

## 🛠️ Requirements

- Java 5+ — `ReentrantLock`, `AtomicInteger`, `ExecutorService`
- Java 8+ — lambda expressions

---

## 📄 License

This project is open-source and available under the [MIT License](../../LICENSE).
