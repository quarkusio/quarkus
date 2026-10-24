---
name: async-programming
description: >
  Idiomatic async patterns for CompletionStage/CompletableFuture, Mutiny
  (Uni/Multi), and Vert.x Future, the antipatterns they replace, and the
  semantic differences to check before rewriting existing async code.
---

# Asynchronous Programming

When writing or modifying asynchronous code in Quarkus, use the idiomatic
operators instead of manual callback wiring. When **rewriting existing code**,
an idiomatic form is only acceptable if it is semantically equivalent to the
code it replaces: most "obvious" rewrites below change observable behaviour
in subtle ways, which are listed with each pattern.

## Refactoring existing code: strict semantic equivalence

Before replacing an async construct, check that the new form preserves:

- **Exception types and wrapping** (raw cause vs `CompletionException`, which
  failure wins when several occur, `CompositeException`).
- **Threads and contexts** on which callbacks, `onSubscribe` and emissions run
  (event loop, worker, virtual thread, duplicated Vert.x context).
- **Eager vs lazy evaluation**: work done when the async value is *assembled*
  vs when it is *subscribed to*, and how many times it runs.
- **Re-subscription and retry safety**: each subscription must start from fresh
  state, exactly like the original code.
- **Concurrency**: ordering, concurrency limits, sequential vs parallel.
- **Completion on all paths**: empty inputs, partial failures, cancellation,
  exceptions thrown inside callbacks, timeouts.
- **Stack depth** for long iterative/recursive chains.

If the rewrite fixes a latent bug (for example a future that never completed
when a callback threw), it is a behaviour change: put it in its own commit with
a reproducer test, never in a refactoring commit.

## General Principles

- **Prefer operators over manual wiring** in new code. A
  `new CompletableFuture<>()` or `Uni.createFrom().emitter()` is a signal to
  look for a built-in operator, but check the equivalence notes before
  replacing one in existing code.
- **Don't mix reactive APIs unnecessarily.** Avoid Uni → CF → Uni
  round-trips. Stay in one API.
- **Don't block on async results inside callbacks or on an event-loop
  thread.** `join()`/`get()`/`await()` block the thread running the callback,
  and deadlock if the awaited result needs that same thread to complete
  (event loop, single-threaded executor, saturated pool). In Quarkus, Mutiny
  `await()` fails fast with `IllegalStateException` on event-loop threads, and
  Vert.x `Future.await()` does so on any Vert.x thread (event loop or worker);
  `CompletableFuture.join()`/`get()` do not and can hang the event loop.
  `join()` on a future known to be complete (e.g. after `allOf`) does not
  block.
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

The same applies to the happy path: use `CompletableFuture.completedFuture(value)`
instead of allocating and calling `complete()`. These rewrites are equivalent:
both forms store the raw exception.

### Exception wrapping

- A stage completed **directly** (`completeExceptionally`, `failedFuture`)
  holds the raw exception.
- A **dependent** stage (`thenApply`, `thenCompose`, `whenComplete`,
  `allOf`, ...) whose source failed, or whose function threw, holds a
  `CompletionException` wrapping it. An existing `CompletionException` is
  relayed, not wrapped again. `handle` and `exceptionally` recover: their
  stage holds the function's result, or a `CompletionException` if the
  function throws.
- Callbacks receive whatever is stored: `exceptionally` called directly on a
  failed future sees the raw exception; one level down it sees a
  `CompletionException`. `whenComplete` passes the stored throwable through
  as-is.
- Unwrap before type checks:
  `Throwable cause = t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;`
- Bridges differ: Mutiny `Uni.createFrom().completionStage(...)` unwraps one
  `CompletionException` level; Vert.x `Future.fromCompletionStage` does not.

### `thenCompose` / `thenApply` instead of `whenComplete` pyramids

```java
// Pyramid: manually forwarding errors at every level
CompletableFuture<R> result = new CompletableFuture<>();
step1().whenComplete((r1, t1) -> {
    if (t1 != null) {
        result.completeExceptionally(t1);
    } else {
        step2(r1).whenComplete((r2, t2) -> { /* same pattern repeated */ });
    }
});
return result;

// Idiomatic: errors propagate automatically
return step1().thenCompose(r1 -> step2(r1));
```

Use this form in new code. In existing code it is **not** a drop-in
replacement:

- The pyramid forwards the raw cause; `thenCompose` makes downstream stages
  see `CompletionException(cause)` (see above). This breaks `instanceof`
  checks in `CompletionStage` callers and in Vert.x code bridged with
  `Future.fromCompletionStage`. Mutiny `onFailure(Type.class)` filters are
  not affected, because the Mutiny bridge unwraps one level.
- The pyramid always returns a fresh, full `CompletableFuture`; the chain
  returns a dependent stage created by `step1()`'s implementation, which may
  be a custom or minimal `CompletionStage` with unsupported methods.
- If code inside a `whenComplete` action throws, the exception goes into the
  discarded dependent stage and `result` **never completes** (a hang).
  `thenCompose` fails the returned stage instead. If you only need to fix
  such a hang while keeping raw causes, wrap the action body in
  `try { ... } catch (Throwable e) { result.completeExceptionally(e); }`.

### Use `exceptionally` / `handle` for error recovery

- `exceptionally(t -> fallback)`: recover from errors with a fallback value.
- `exceptionallyCompose(t -> asyncFallback())`: recover with an async step
  (same compose vs apply distinction as `thenCompose` vs `thenApply`).
- `handle((v, t) -> ...)`: transform both success and failure in one callback.

Do not use `whenComplete` for error recovery: it cannot change the result.
Remember that `t` may be a `CompletionException` (see above).

### Use `allOf` for parallel fan-out

```java
CompletableFuture.allOf(cf1, cf2, cf3)
        .thenApply(v -> combine(cf1.join(), cf2.join(), cf3.join()));
```

Compared to manual counting with `whenComplete` callbacks, `allOf` completes
only when **all** inputs have completed, even after one has failed, and its
failure is a `CompletionException` wrapping the cause. A manual version that
completes exceptionally on the first failure behaves differently.

## Vert.x Future

### Listeners vs transformations

- `onSuccess`, `onFailure` and `onComplete` register **terminal listeners** and
  return the *same* future. Their execution order is not guaranteed, and an
  exception thrown by a listener does not fail the future, so a throw in
  `onSuccess` never reaches `onFailure`. It is reported to the context
  exception handler; for a future without a context (e.g. from
  `Promise.promise()`) it is effectively dropped.
- Use `compose`/`map`/`transform`/`recover`/`otherwise` to change the outcome,
  `andThen` for ordered side effects, and `eventually` for cleanup.
- Do not do the real work of a step inside a listener while also returning
  the future from `compose`: the chain then completes independently of that
  work.
- To carry an earlier value forward, nest the transformation
  (`resp -> resp.body().map(b -> new Result(resp, b))`) instead of stashing it
  in an `AtomicReference` side channel.
- A `compose` mapper must never return `null`.

### Use `compose` instead of nested `onComplete`

```java
// Pyramid with duplicated error handling
futureA.onComplete(ar1 -> {
    if (ar1.failed()) {
        handleError(ar1.cause());
        return;
    }
    futureB(ar1.result()).onComplete(ar2 -> { /* same pattern */ });
});

// Flat chain, errors short-circuit automatically
futureA.compose(a -> futureB(a))
        .onComplete(b -> done(b), t -> handleError(t));
```

Equivalence notes: `compose` relays the same `Throwable` instance. But if the
code between two steps can throw synchronously, the pyramid let the exception
escape (often leaving the operation hanging), while `compose` turns it into a
failure of the chain. Also keep side effects in their original order (e.g.
`resume()` of a paused stream after the handlers that consume it are set).

### Factory methods and fan-out

- Use `Future.succeededFuture(value)` and `Future.failedFuture(e)`, like the
  CompletableFuture factories.
- `Future.all(...)` fails as soon as one input fails; `Future.join(...)` waits
  for all inputs to complete; `Future.any(...)` succeeds on the first success.
- Use `Promise.promise()` only at callback boundaries, as the Vert.x
  equivalent of `new CompletableFuture<>()`.

## Mutiny: Uni

### Creation, eager vs lazy evaluation

- `Uni.createFrom().item(compute())` runs `compute()` **at assembly time**,
  once, on the caller thread. `Uni.createFrom().item(() -> compute())` runs it
  **per subscription**. The same applies to `failure(e)` vs
  `failure(() -> e)` and `completionStage(cf)` vs `completionStage(() -> cf())`.
  `item(value)` is fine for an already-computed local value.
- **Use `item(Supplier)` instead of a manual emitter** for synchronous
  computations: exceptions thrown by the supplier become failures.
- **Use `voidItem()`** instead of `item((Void) null)`.
- **Don't wrap a known value in a CompletableFuture**: use
  `Uni.createFrom().item(value)` instead of
  `Uni.createFrom().completionStage(() -> CompletableFuture.completedFuture(value))`.
- `deferred(() -> methodReturningUni())` is redundant inside `chain`/`call`/
  `transformToUni`, whose mappers already run per subscription. Elsewhere, use
  it whenever building the Uni has side effects or captures state (fires I/O,
  wraps an already-started `CompletableFuture`, reads mutable state), otherwise
  the Uni is not retry/re-subscription safe.

### Retry and re-subscription safety

`retry()`, `repeat()` and multiple subscribers re-subscribe upstream. Anything
that must start fresh for each attempt (cursors, accumulators, counters) has to
be created inside `item(Supplier)` or `deferred(...)`, **per subscription**.
A field of a supplier object, or a variable captured outside them, is shared
by all subscriptions, including concurrent ones. Note that `Uni.createFrom().deferred(stateSupplier, mapper)` and
`Multi.createBy().repeating().uni(stateSupplier, mapper)` call the state
supplier once per *operator instance*, not per subscription.

### Threading: `runSubscriptionOn` and `emitOn`

In new code, prefer the operators to manual executor wiring:

```java
// Manual executor wiring
Uni.createFrom().emitter(em -> executor.execute(() -> {
    try {
        em.complete(supplier.get());
    } catch (Throwable t) {
        em.fail(t);
    }
}));

// Operator
Uni.createFrom().item(supplier).runSubscriptionOn(executor);
```

These are **not** equivalent: with `runSubscriptionOn`, `onSubscribe` (and
`onSubscription()` callbacks) run on the executor thread and the subscriber
cannot cancel before the task starts, and `item(Supplier)` decorates the
supplier with context propagation. An emitter that bridges an `Executor` is
already an acceptable idiom; do not rewrite it in existing code without a
reason.

For dispatching results back to a Vert.x context:

```java
// io.vertx.core.Context
uni.emitOn(cmd -> ctx.runOnContext(ignored -> cmd.run()));
// io.vertx.mutiny.core.Context
uni.emitOn(cmd -> ctx.runOnContext(cmd));
```

`emitOn` differs from the `onItemOrFailure().transformToUni(emitter +
runOnContext)` pattern: if the dispatch is rejected (closed event loop) after
an upstream failure, `emitOn` reports a `CompositeException` of both instead
of the rejection alone. Keep the original pattern when that matters.

When offloading with `runSubscriptionOn(blockingExecutor)`, results continue
on the worker thread. If downstream code needs the Vert.x context (HTTP
response writing, context locals, non-thread-safe state), add `emitOn` with
the context captured on the calling thread (`Vertx.currentContext()`, which
may be `null`), which must be the request's duplicated context.
`emitOn` dispatches failures too. Request-scoped CDI beans are not safe to
use after hopping to another executor.

### Chaining

**Use `call()` instead of `chain()` + `replaceWith()`:**

```java
// Before
uni.chain(v -> sideEffect(v).replaceWith(v))

// After: call() keeps the upstream item
uni.call(v -> sideEffect(v))
```

This rewrite is equivalent: `call` is implemented as
`transformToUni(item -> f(item).onItem().transform(ignored -> item))`, plus a
null check on the returned Uni and `Infrastructure.decorate` on the function.
`flatMap` is a synonym for `chain`, so the same rule applies to
`flatMap(v -> sideEffect(v).replaceWith(v))`.

### Iterating asynchronously: use `repeating()`, not recursion

```java
// WRONG: emitter passed through recursive subscriptions; exceptions thrown
// in the callback are dropped and the emitter never completes
void scanKeys(String cursor, Set<String> acc, UniEmitter<Set<String>> em) {
    doWork(cursor).subscribe().with(resp -> {
        acc.addAll(keys(resp));
        if (done(resp)) {
            em.complete(acc);
        } else {
            scanKeys(next(resp), acc, em);
        }
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

Recursive `chain` (`return doWork(c).chain(r -> done ? item(acc) : scanKeys(next))`)
is not a safe replacement: nested `transformToUni` is not trampolined, so each
iteration adds stack frames when steps complete synchronously, and the final
item travels through one operator per iteration. `Multi.createBy().repeating()`
trampolines.

### Conversion: don't round-trip between APIs

```java
// Round-trip through a CompletableFuture
CompletableFuture<T> cf = new CompletableFuture<>();
uni.subscribe().with(cf::complete, cf::completeExceptionally);
return Uni.createFrom().completionStage(cf);

// Stay in Uni
return uni;
```

In new code, stay in Uni. When rewriting existing code, note that the
round-trip subscribes **eagerly**, once, even if nobody subscribes to the
result, while `return uni` is lazy and runs again for each subscriber.
Cancelling the bridged Uni only cancels the CompletableFuture, not the original
subscription. The bridge does not switch threads: `completionStage(cf)` emits
on the thread that completes `cf`, or on the subscribing thread if `cf` is
already complete.

### Waiting for several Unis

`Uni.join().all(unis)` subscribes to all Unis concurrently. `andFailFast()`
fails on the first failure and cancels the others; `andCollectFailures()`
waits for all of them and reports a `CompositeException`, even when only one
failed.

Replacing a `CountDownLatch` with `Uni.join().all(...).andCollectFailures().await()`
is not equivalent: `await()` throws `IllegalStateException` on an event-loop
thread before subscribing to anything, wraps checked exceptions such as
`InterruptedException` in `CompletionException`, and `.atMost(d)` adds a
timeout that cancels the Unis. Keep the latch, or treat the change as a
behaviour change.

## Mutiny: Multi

### `transformToUni(...).merge()` instead of emitter + manual counting

```java
// Manual completion tracking
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

// Operator
Multi.createFrom().iterable(list)
        .onItem().transformToUni(item -> process(item))
        .merge(Math.max(1, list.size()))
```

Notes:

- `transformToUniAndMerge` and `merge()` limit concurrency to
  `Infrastructure.getBufferSizeS()` (256 by default); pass the concurrency
  explicitly to keep "all at once".
- The manual version above never completes when a Uni fails (the failure is
  dropped) or when the list is empty; the operator propagates the failure,
  completes on empty input, and cancels in-flight Unis when cancelled. That is
  a bug fix, not a refactoring.

### Subscriptions without a failure handler

An item-only subscriber (`subscribe().with(onItem)`) does not handle failures:

- on a `Uni`, the failure goes to Mutiny's dropped-exception handler, which
  Quarkus logs at ERROR with a generic message;
- on a `Multi`, it is printed to `System.err` ("Missing onFailure/onError
  handler"), bypassing the logging configuration.

In neither case can the code react (retry, clean up, propagate, complete a
pending result). Provide a failure consumer whenever the failure must be
observed or acted upon:

```java
multi.subscribe().with(
        item -> handle(item),
        t -> LOG.error("subscription error", t))
```

Adding one to existing code changes the log destination, level and message,
so it is a behaviour change, not a refactoring.
