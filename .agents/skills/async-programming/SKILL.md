---
name: async-programming
description: >
  Idiomatic async patterns for CompletionStage/CompletableFuture, Mutiny
  (Uni/Multi), and Vert.x Future, the antipatterns they replace, and the
  semantic differences to check before rewriting existing async code.
---

# Asynchronous Programming

Prefer idiomatic operators over manual callback wiring in new code — but manual
wiring (e.g. `whenComplete` pyramids) is the right choice when it preserves
semantics that operators would break. When **rewriting existing code**, an
idiomatic form is only acceptable if it is semantically equivalent; most
"obvious" rewrites change observable behaviour in subtle ways.

## Rewriting existing code: semantic equivalence checklist

Before replacing an async construct, verify that the new form preserves:

- **Exception types and wrapping** (raw cause vs `CompletionException`; which
  failure wins when several occur; `CompositeException`).
- **Threads and contexts** on which callbacks run (event loop, worker, virtual
  thread, duplicated Vert.x context).
- **Eager vs lazy evaluation** and how many times the work runs.
- **Re-subscription and retry safety**: fresh state per attempt.
- **Concurrency**: ordering, limits, sequential vs parallel.
- **Completion on all paths**: empty inputs, partial failures, cancellation,
  thrown exceptions, timeouts.
- **Stack depth** for long iterative/recursive chains.

If the rewrite fixes a latent bug (e.g. a future that never completed when a
callback threw), it is a behaviour change: put it in its own commit with a
reproducer test, never in a refactoring commit.

## Quick-reference decision rules

| You see | Do this | Watch out |
|---------|---------|-----------|
| `new CF<>()` + `completeExceptionally(e)` | `CompletableFuture.failedFuture(e)` | Safe — same wrapping |
| `new CF<>()` + `complete(v)` | `CompletableFuture.completedFuture(v)` | Safe — same wrapping |
| `whenComplete` pyramid chaining steps | Keep if downstream needs raw causes or bridges to Vert.x `fromCompletionStage`; replace with `thenCompose` chain otherwise | Chain wraps in `CompletionException`; pyramid preserves raw cause |
| `whenComplete` used for error recovery | Use `exceptionally` or `handle` instead | `whenComplete` cannot change the result |
| Manual counting for parallel CFs | `CompletableFuture.allOf(...)` | `allOf` waits for **all** inputs even after failure — differs from fail-fast manual versions |
| `cf.join()`/`get()` inside a callback | Don't — deadlocks if the awaited result needs the same thread | Mutiny `await()` and Vert.x `Future.await()` fail fast; CF `join()`/`get()` silently hang. `join()` after `allOf` is safe (future already complete) |
| Nested Vert.x `onComplete` pyramid | `compose` chain | `compose` catches sync throws (pyramid lets them escape, operation hangs) |
| Vert.x listener doing real work | Move into `compose`/`map`/`andThen` | Listener exceptions don't fail the future; dropped without a context |
| `Future.fromCompletionStage(cs)` | Unwrap `CompletionException` before bridging if consumer does `instanceof` on the cause | `fromCompletionStage` does **not** unwrap; `toCompletionStage` passes raw cause |
| `Uni.createFrom().emitter()` for sync work | `Uni.createFrom().item(supplier)` | Emitter is acceptable at callback boundaries; don't rewrite existing ones without reason |
| `chain(v -> x(v).replaceWith(v))` | `call(v -> x(v))` | Equivalent — safe rewrite |
| Identity emitter wrapping a Uni | Remove the wrapper, return the Uni directly | Check that no branch actually needs `emitOn` dispatch |
| Recursive `chain` for iteration | `Multi.createBy().repeating().uni(...)` | Recursive `chain` is not trampolined — stack overflow on sync completions |
| `Uni → CF → Uni` round-trip | Stay in Uni | Round-trip subscribes eagerly; `return uni` is lazy |
| Manual `emitter` + counting for Multi | `transformToUni(...).merge(concurrency)` | `merge()` defaults to 256 concurrency; pass explicit value |
| `subscribe().with(onItem)` — no failure handler | Add failure consumer | Without one: Uni logs at ERROR; Multi prints to stderr |

## CompletionStage / CompletableFuture

### Factory methods

```java
// WRONG
CompletableFuture<T> cf = new CompletableFuture<>();
cf.completeExceptionally(e);
return cf;

// RIGHT
return CompletableFuture.failedFuture(e);
```

Same for the happy path: `CompletableFuture.completedFuture(value)`.
Both forms store the raw exception — this is a safe rewrite.

### Chaining sequential async steps

Two valid idioms. Choose based on whether downstream consumers need raw
exception types.

```java
// Chain: errors propagate automatically, wrapped in CompletionException
return step1().thenCompose(r1 -> step2(r1));

// Pyramid: manually forwarding errors, preserving raw causes
CompletableFuture<R> result = new CompletableFuture<>();
step1().whenComplete((r1, t1) -> {
    if (t1 != null) {
        result.completeExceptionally(t1);
    } else {
        step2(r1).whenComplete((r2, t2) -> { /* same pattern repeated */ });
    }
});
return result;
```

**Use the chain** when no downstream consumer does `instanceof` on the cause
and the result is not bridged to Vert.x via `Future.fromCompletionStage`.

**Use the pyramid** when downstream code relies on raw exception types, or
when bridging to Vert.x (`fromCompletionStage` does **not** unwrap
`CompletionException`). Mutiny `onFailure(Type.class)` filters work with
either form when the `CompletionStage` enters via
`Uni.createFrom().completionStage(...)` (that bridge unwraps one
`CompletionException` level).

Replacing a pyramid with a chain in existing code is **not** a refactoring:

- The pyramid forwards the raw cause; `thenCompose` wraps it.
- The pyramid returns a full `CompletableFuture`; the chain returns whatever
  `step1()`'s implementation creates (may be a minimal `CompletionStage`).
- If a `whenComplete` action throws, `result` **never completes** (hang).
  `thenCompose` fails the stage instead. To fix a hang while keeping raw
  causes, wrap the body in
  `try { ... } catch (Throwable e) { result.completeExceptionally(e); }`.

### Error recovery

- `exceptionally(t -> fallback)` — recover with a fallback value.
- `exceptionallyCompose(t -> asyncFallback())` — recover with an async step.
- `handle((v, t) -> ...)` — transform both success and failure.

`whenComplete` cannot change the result — don't use it for recovery.
`t` may be a `CompletionException` — unwrap before type checks (see
reference below).

### Parallel fan-out with `allOf`

```java
CompletableFuture.allOf(cf1, cf2, cf3)
        .thenApply(v -> combine(cf1.join(), cf2.join(), cf3.join()));
```

`allOf` completes only when **all** inputs complete, even after one fails.
Its failure is a `CompletionException`. A manual version that fails on the
first failure behaves differently. `join()` inside `thenApply` after `allOf`
is safe — the futures are already complete, so it does not block.

## Vert.x Future

### Listeners vs transformations

- `onSuccess`/`onFailure`/`onComplete` are **terminal listeners**: they return
  the *same* future, run in unspecified order, and a thrown exception does not
  fail the future (reported to the context exception handler; dropped without
  a context).
- Use `compose`/`map`/`transform`/`recover`/`otherwise` to change the outcome,
  `andThen` for ordered side effects, `eventually` for cleanup.
- Don't do real work in a listener while also chaining with `compose` — the
  chain completes independently.
- Carry values forward by nesting transformations, not `AtomicReference`.
- A `compose` mapper must never return `null`.

### `compose` instead of nested `onComplete`

```java
// Pyramid with duplicated error handling
futureA.onComplete(ar1 -> {
    if (ar1.failed()) { handleError(ar1.cause()); return; }
    futureB(ar1.result()).onComplete(ar2 -> { /* same pattern */ });
});

// Flat chain
futureA.compose(a -> futureB(a))
        .onComplete(b -> done(b), t -> handleError(t));  // two-handler overload: Vert.x 5+
```

`compose` relays the same `Throwable` instance. If code between steps can
throw synchronously, the pyramid lets the exception escape as an uncaught
exception and the promise is never completed (hang); `compose` catches it and
fails the chain. Keep side effects in their original order.

### Factory methods and fan-out

- `Future.succeededFuture(value)` / `Future.failedFuture(e)`.
- `Future.all(...)` — fail-fast; `Future.join(...)` — wait for all;
  `Future.any(...)` — first success.
- `Promise.promise()` only at callback boundaries.

### Bridging Vert.x Future ↔ CompletionStage

- `Future.fromCompletionStage(cs)` does **not** unwrap `CompletionException`.
  Unwrap before bridging if the consumer does `instanceof` on the cause.
- `future.toCompletionStage()` completes on the thread that completes the
  Vert.x future (typically the event-loop for context-bound futures). Its
  failure is the raw `Throwable`; dependent stages wrap it as usual.

## Mutiny: Uni

### Creation

- `Uni.createFrom().item(compute())` — runs at **assembly time**, once.
  `item(() -> compute())` — runs **per subscription**. Same for `failure()`
  and `completionStage()` variants. `item(value)` is fine for an
  already-computed value.
- Use `item(Supplier)` over a manual emitter for synchronous computations.
- Use `voidItem()` instead of `item((Void) null)`.
- Don't wrap a known value in a CF: use `item(value)` directly.
- `deferred(() -> uniReturningMethod())` is redundant inside `chain`/`call`/
  `transformToUni` (those mappers already run per subscription). Use it
  elsewhere when building the Uni has side effects or captures mutable state.

### Retry and re-subscription safety

`retry()`, `repeat()`, and multiple subscribers re-subscribe upstream. State
that must be fresh per attempt (cursors, accumulators) must be created inside
`item(Supplier)` or `deferred(...)`, **per subscription**. Captured variables
are shared across subscriptions. `deferred(stateSupplier, mapper)` and
`repeating().uni(stateSupplier, mapper)` call the state supplier once per
*operator instance*, not per subscription.

### Threading: `runSubscriptionOn` and `emitOn`

```java
// Manual executor wiring (acceptable idiom — don't rewrite without reason)
Uni.createFrom().emitter(em -> executor.execute(() -> {
    try { em.complete(supplier.get()); }
    catch (Throwable t) { em.fail(t); }
}));

// Operator (preferred in new code)
Uni.createFrom().item(supplier).runSubscriptionOn(executor);
```

Not equivalent: `runSubscriptionOn` runs `onSubscribe` on the executor thread,
prevents cancellation before start, and `item(Supplier)` adds context
propagation.

Dispatching results back to a Vert.x context:

```java
uni.emitOn(cmd -> ctx.runOnContext(ignored -> cmd.run()));  // io.vertx.core.Context
uni.emitOn(cmd -> ctx.runOnContext(cmd));                   // io.vertx.mutiny.core.Context
```

`emitOn` differs from `onItemOrFailure().transformToUni(emitter + runOnContext)`:
if dispatch is rejected after an upstream failure, `emitOn` reports a
`CompositeException` of both. Keep the original pattern when that matters.

When offloading with `runSubscriptionOn(blockingExecutor)`, results stay on
the worker thread. If downstream needs the Vert.x context, add `emitOn` with
the context captured on the calling thread (`Vertx.currentContext()`). `emitOn`
dispatches failures too. Request-scoped CDI beans are not safe after hopping.

### Chaining: `call()` vs `chain()` + `replaceWith()`

```java
// Before
uni.chain(v -> sideEffect(v).replaceWith(v))

// After — equivalent, call() keeps the upstream item
uni.call(v -> sideEffect(v))
```

`flatMap` is a synonym for `chain` — same rule applies.

### Don't wrap a Uni in a no-op identity emitter

```java
// WRONG
return uni.onItemOrFailure().transformToUni((item, failure) -> Uni.createFrom().emitter(emitter -> {
    if (failure != null) { emitter.fail(failure); } else { emitter.complete(item); }
}));

// RIGHT
return uni;
```

### Iterating asynchronously: `repeating()`, not recursion

```java
// WRONG: dropped exceptions, emitter never completes on throw
void scanKeys(String cursor, Set<String> acc, UniEmitter<Set<String>> em) {
    doWork(cursor).subscribe().with(resp -> {
        acc.addAll(keys(resp));
        if (done(resp)) em.complete(acc);
        else scanKeys(next(resp), acc, em);
    });
}

// RIGHT: trampolined loop with per-subscription state
Uni<Set<String>> scanKeys() {
    return Uni.createFrom().deferred(() -> {
        AtomicReference<String> cursor = new AtomicReference<>("0");
        return Multi.createBy().repeating()
                .uni(() -> doWork(cursor.get()).map(resp -> {
                    cursor.set(next(resp));
                    return keys(resp);
                }))
                .whilst(keys -> !"0".equals(cursor.get()))
                .collect().in(HashSet::new, Set::addAll);
    });
}
```

Recursive `chain` is not trampolined — each iteration adds stack frames when
steps complete synchronously. `Multi.createBy().repeating()` trampolines.

### Don't round-trip between APIs

```java
// WRONG: round-trip subscribes eagerly, once
CompletableFuture<T> cf = new CompletableFuture<>();
uni.subscribe().with(cf::complete, cf::completeExceptionally);
return Uni.createFrom().completionStage(cf);

// RIGHT: lazy, runs per subscriber
return uni;
```

The round-trip also breaks cancellation (cancelling the bridged Uni only
cancels the CF, not the original subscription) and does not switch threads.

### Waiting for several Unis

`Uni.join().all(unis)` subscribes concurrently. `andFailFast()` cancels on
first failure; `andCollectFailures()` waits for all and reports
`CompositeException`.

Replacing a `CountDownLatch` with `Uni.join().all(...).await()` is a behaviour
change: `await()` throws `IllegalStateException` on event-loop threads, wraps
checked exceptions in `CompletionException`, and `.atMost(d)` adds a timeout.

## Mutiny: Multi

### `transformToUni(...).merge()` instead of emitter + manual counting

```java
// Manual — never completes on failure or empty list
Multi.createFrom().emitter(em -> {
    AtomicInteger count = new AtomicInteger();
    for (var item : list) {
        process(item).subscribe().with(r -> {
            em.emit(r);
            if (count.incrementAndGet() == list.size()) em.complete();
        });
    }
})

// Operator
Multi.createFrom().iterable(list)
        .onItem().transformToUni(item -> process(item))
        .merge(Math.max(1, list.size()))
```

`merge()` defaults to 256 concurrency — pass the value explicitly to keep
"all at once." The operator propagates failures (the manual version drops
them), completes on empty input, and cancels in-flight Unis — those are bug
fixes, not refactorings.

### Subscriptions without a failure handler

`subscribe().with(onItem)` without a failure consumer: on Uni, failures go to
Mutiny's dropped-exception handler (logged at ERROR); on Multi, printed to
stderr bypassing logging. Provide a failure consumer when the failure must be
observed. Adding one to existing code changes log destination and level — it
is a behaviour change.

## Reference: CompletionException wrapping rules

- A stage completed **directly** (`completeExceptionally`, `failedFuture`)
  stores the raw exception.
- A **dependent** stage (`thenApply`, `thenCompose`, `whenComplete`, `allOf`,
  ...) wraps it in `CompletionException`. An existing `CompletionException` is
  relayed, not wrapped again.
- `handle` and `exceptionally` recover: their stage stores the function's
  result, or a `CompletionException` if the function throws.
- Callbacks receive whatever is stored: `exceptionally` on a directly-failed
  future sees the raw exception; one level down, `CompletionException`.
  `whenComplete` passes the stored throwable as-is.
- Unwrap before type checks:
  `Throwable cause = t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;`
- Bridges: `Uni.createFrom().completionStage(...)` unwraps one
  `CompletionException` level; Vert.x `Future.fromCompletionStage` does not.
