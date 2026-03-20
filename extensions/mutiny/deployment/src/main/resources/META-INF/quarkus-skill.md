### Uni vs Multi

- `Uni<T>` — 0 or 1 result (like CompletableFuture). Use for single-value async operations.
- `Multi<T>` — 0 to N results (like reactive stream). Use for collections/streams.

### Transformations

- `uni.onItem().transform(item -> ...)` — map the result.
- `uni.onFailure().recoverWithItem(fallback)` — handle errors.
- `uni.chain(item -> anotherUni)` — chain async operations (flatMap).
- `Multi.createFrom().items(a, b, c)` — create from values.

### Combining

- `Uni.combine().all().unis(uni1, uni2).asTuple()` — wait for multiple.
- `Multi.createBy().merging().streams(m1, m2)` — merge streams.

### Testing

- Use `UniAssertSubscriber`: `uni.subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted()`.
- Use `.await().atMost(Duration.ofSeconds(5))` in tests only.

### Common Pitfalls

- Do NOT call `.await().indefinitely()` in production — it blocks the thread.
- Do NOT block on Vert.x event loop threads — use Mutiny operators.
- `Uni` is lazy — nothing happens until subscribed.
