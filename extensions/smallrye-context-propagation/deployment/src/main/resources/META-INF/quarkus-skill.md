
### Usage

The extension is included transitively by most reactive extensions. It ensures CDI request context, transaction context, and other thread-local state propagates correctly across async boundaries (reactive pipelines, `Uni`/`Multi`).

In most cases, **no code changes are needed** — context propagation works automatically.

### When You Need It Explicitly

Add this extension when:
- Using `CompletionStage`, `CompletableFuture` or `ExecutorService` manually and CDI context is lost
- Running async code that needs `@RequestScoped` beans or `@Transaction` context

### ManagedExecutor

Inject a context-aware executor instead of creating raw thread pools:

```java
@Inject ManagedExecutor executor;

public CompletionStage<String> asyncWork() {
    return executor.supplyAsync(() -> {
        // CDI request context is available here
        return myRequestScopedBean.getValue();
    });
}
```

### ThreadContext

Wrap tasks to propagate context when using executors not managed by Quarkus:

```java
@Inject ThreadContext threadContext;

Runnable wrapped = threadContext.contextualRunnable(() -> {
    // CDI context propagated from the calling thread
});
someExternalExecutor.execute(wrapped);
```

Also available: `contextualCallable()`, `contextualConsumer()`, `contextualFunction()`, `contextualSupplier()`.

### Configuration

```properties
# Control which contexts propagate (rarely needed)
mp.context.ManagedExecutor.propagated=CDI,Transaction
mp.context.ManagedExecutor.cleared=Remaining
mp.context.ManagedExecutor.maxAsync=5
mp.context.ManagedExecutor.maxQueued=20
```

### Common Pitfalls

- **Usually not needed as a direct dependency**: Most reactive extensions (REST, reactive messaging, etc.) include it transitively. Add explicitly only if CDI context is lost in async code.
- **Don't create raw `ExecutorService`**: Use `@Inject ManagedExecutor` instead — it propagates CDI context. Raw `Executors.newFixedThreadPool()` loses all context.
- **`@RequestScoped` in async code**: Without context propagation, `@RequestScoped` beans throw `ContextNotActiveException` on other threads. This extension fixes that.
- **Transaction propagation**: Transactions propagate to async tasks by default. Be aware this means a `@Transactional` method's transaction extends to `ManagedExecutor` tasks.
