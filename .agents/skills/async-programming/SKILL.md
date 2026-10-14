---
name: async-programming
description: >
  Idiomatic async patterns for CompletionStage/CompletableFuture, Mutiny
  (Uni/Multi), and Vert.x Future. Common antipatterns and their fixes.
---

# Asynchronous Programming

When writing or modifying asynchronous code in Quarkus, use the idiomatic
operators instead of manual callback wiring. The patterns below are real
antipatterns found (and fixed) in this codebase.

## General Principles

- **Prefer operators over manual wiring.** Every `new CompletableFuture<>()`
  or `Uni.createFrom().emitter()` is a signal to look for a built-in
  operator that does the same thing with correct error propagation.
- **Don't mix reactive APIs unnecessarily.** Avoid Uni → CF → Uni
  round-trips. Stay in one API.
- **Never block inside an async callback.** Calling `.await()`, `.join()`,
  or `.get()` inside `thenCompose`, `chain`, or any reactive operator
  deadlocks the thread the chain runs on.
- **Extract duplicate async logic into helpers.** If the same retry/error
  handling block appears in multiple callbacks, extract a method.

## CompletionStage / CompletableFuture

### Factory methods

Use the static factories instead of allocate-then-complete:

```java
// WRONG
CompletableFuture<T> cf = new CompletableFuture<>();
cf.completeExceptionally(e);
return cf;

// RIGHT
return CompletableFuture.failedFuture(e);
```

The same applies to the happy path — use `CompletableFuture.completedFuture(value)`
instead of allocating and calling `complete()`.

### Use `thenCompose` / `thenApply` instead of `whenComplete` pyramids

```java
// WRONG — manually forwarding errors at every level
CompletableFuture<R> result = new CompletableFuture<>();
step1().whenComplete((r1, t1) -> {
    if (t1 != null) {
        result.completeExceptionally(t1);
    } else {
        step2(r1).whenComplete((r2, t2) -> { /* same pattern repeated */ });
    }
});
return result;

// RIGHT — errors propagate automatically
return step1().thenCompose(r1 -> step2(r1));
```

Use `thenApply` for synchronous transforms, `thenCompose` when returning
another `CompletionStage`. Never allocate a `CompletableFuture` just to
manually forward results and errors.

### Use `exceptionally` / `handle` for error recovery

- `exceptionally(t -> fallback)` — recover from errors with a fallback value.
- `exceptionallyCompose(t -> asyncFallback())` — recover with an async step
  (same compose vs apply distinction as `thenCompose` vs `thenApply`).
- `handle((v, t) -> ...)` — transform both success and failure in one callback.

Do not use `whenComplete` for error recovery — it cannot change the result.

### Use `allOf` for parallel fan-out

```java
// WRONG — manual counting
CompletableFuture<R> result = new CompletableFuture<>();
AtomicInteger remaining = new AtomicInteger(3);
BiConsumer<Object, Throwable> callback = (v, t) -> {
    if (t != null) {
        result.completeExceptionally(t);
    } else if (remaining.decrementAndGet() == 0) {
        result.complete(combine(cf1.join(), cf2.join(), cf3.join()));
    }
};
cf1.whenComplete(callback);
cf2.whenComplete(callback);
cf3.whenComplete(callback);
return result;

// RIGHT
CompletableFuture.allOf(cf1, cf2, cf3)
        .thenApply(v -> combine(cf1.join(), cf2.join(), cf3.join()));
```

### Never block inside async callbacks

```java
// WRONG — deadlocks
step1().thenCompose(r -> {
    Result r2 = step2(r).join(); // blocks the thread running the chain
    return step3(r2);
});

// RIGHT
step1().thenCompose(r -> step2(r))
        .thenCompose(r2 -> step3(r2));
```

## Vert.x Future

### Use `compose` instead of nested `onComplete`

```java
// WRONG — callback pyramid with duplicated error handling
futureA.onComplete(ar1 -> {
    if (ar1.failed()) {
        handleError(ar1.cause());
        return;
    }
    futureB(ar1.result()).onComplete(ar2 -> { /* same pattern */ });
});

// RIGHT — flat chain, errors short-circuit automatically
futureA.compose(a -> futureB(a))
        .onSuccess(b -> done(b))
        .onFailure(t -> handleError(t));
```

### Factory methods and fan-out

- Use `Future.succeededFuture(value)` and `Future.failedFuture(e)` —
  same principle as the CompletableFuture factories.
- Use `Future.all(f1, f2, f3)` or `Future.any(f1, f2)` for parallel
  operations instead of manual counting.

## Mutiny — Uni

### Creation

- **Use `item(Supplier)` instead of manual emitter** —
  `Uni.createFrom().item(() -> compute())` catches exceptions automatically.
  No need for `emitter(em -> { try { em.complete(...) } catch ... })`.

- **Use `voidItem()`** — write `Uni.createFrom().voidItem()` instead of
  `Uni.createFrom().item((Void) null)`.

- **Don't wrap a lazy Uni in `deferred()`** —
  `Uni.createFrom().deferred(() -> methodReturningUni())` is redundant when
  the call is already inside a lazy operator (`chain()`, `call()`, etc.).
  `deferred()` is only needed when the Uni construction itself has side
  effects and happens in an eager context (field initializer, constructor).

- **Don't wrap a known value in CompletableFuture for Uni** — use
  `Uni.createFrom().item(value)` instead of
  `Uni.createFrom().completionStage(() -> CompletableFuture.completedFuture(value))`.

### Threading — `runSubscriptionOn` and `emitOn`

**Use `runSubscriptionOn()` instead of emitter + executor dispatch:**

```java
// WRONG — manual executor wiring
Uni.createFrom().emitter(em -> {
    executor.execute(() -> {
        try {
            em.complete(supplier.get());
        } catch (Throwable t) {
            em.fail(t);
        }
    });
})

// RIGHT
Uni.createFrom().item(supplier).runSubscriptionOn(executor)
```

**Use `emitOn()` for context dispatch:**

```java
// WRONG — manual item/failure forwarding
uni.onItemOrFailure().transformToUni((item, failure) ->
        Uni.createFrom().emitter(em -> {
            ctx.runOnContext(() -> { /* branch on failure, call em */ });
        }))

// RIGHT
uni.emitOn(cmd -> ctx.runOnContext(cmd))
```

**Correctness: always pair `runSubscriptionOn()` with `emitOn()` when the
caller needs the Vert.x event loop.** When offloading to a worker with
`runSubscriptionOn(blockingExecutor)`, the result continues on the worker
thread. If downstream code requires the event loop (HTTP response writing,
context locals, non-thread-safe state), add `emitOn()` after:

```java
// WRONG — result stays on the worker thread
return Uni.createFrom().item(supplier)
        .runSubscriptionOn(blockingExecutor);

// RIGHT — result dispatched back to the event loop
return Uni.createFrom().item(supplier)
        .runSubscriptionOn(blockingExecutor)
        .emitOn(cmd -> vertxContext.runOnContext(cmd));
```

### Chaining

**Use `call()` instead of `chain()` + `replaceWith()`:**

```java
// WRONG
uni.chain(v -> sideEffect(v).replaceWith(v))

// RIGHT — call() preserves upstream item automatically
uni.call(v -> sideEffect(v))
```

Note: `flatMap` is a synonym for `chain` in Mutiny — the same rule applies
to `flatMap(v -> sideEffect(v).replaceWith(v))`.

**Use `chain()` recursion, not emitter-passing recursion:**

```java
// WRONG — passing an emitter through recursive calls
void scanKeys(String cursor, UniEmitter<Result> emitter) {
    doWork(cursor).subscribe().with(resp -> {
        if (done) {
            emitter.complete(result);
        } else {
            scanKeys(nextCursor, emitter);
        }
    });
}

// RIGHT — returning Uni, composable and cancellable
Uni<Result> scanKeys(String cursor) {
    return doWork(cursor).chain(resp -> {
        if (done) {
            return Uni.createFrom().item(result);
        } else {
            return scanKeys(nextCursor);
        }
    });
}
```

### Conversion — don't round-trip between APIs

```java
// WRONG — loses cancellation, adds allocation
CompletableFuture<T> cf = new CompletableFuture<>();
uni.subscribe().with(cf::complete, cf::completeExceptionally);
return Uni.createFrom().completionStage(cf);

// RIGHT — stay in Uni; add emitOn() only if thread dispatch was the reason
return uni;
// or, if the CF bridge was for thread dispatch:
return uni.emitOn(executor);
```

### Use `Uni.join()` instead of `CountDownLatch`

```java
// WRONG — blocks thread, no error propagation, no timeout
CountDownLatch latch = new CountDownLatch(n);
for (Uni<?> uni : unis) {
    uni.subscribe().with(v -> latch.countDown());
}
latch.await();

// RIGHT
Uni.join().all(uniList).andFailFast().await().atMost(Duration.ofSeconds(30))
```

## Mutiny — Multi

### Use `transformToUniAndMerge` instead of emitter + manual counting

```java
// WRONG — error-prone manual completion tracking
Multi.createFrom().emitter(em -> {
    AtomicInteger count = new AtomicInteger();
    for (var item : list) {
        process(item).subscribe().with(r -> {
            em.emit(r);
            if (count.incrementAndGet() == list.size()) {
                em.complete();
            }
        });
    }
})

// RIGHT
Multi.createFrom().iterable(list)
        .onItem().transformToUniAndMerge(item -> process(item))
```

### Always add error handlers on subscriptions

Unhandled errors on subscriptions silently swallow failures. Always
provide an error consumer, especially on broadcast processors:

```java
// WRONG — silently swallows errors
multi.subscribe().with(item -> handle(item))

// RIGHT
multi.subscribe().with(
        item -> handle(item),
        t -> LOG.error("subscription error", t))
```
