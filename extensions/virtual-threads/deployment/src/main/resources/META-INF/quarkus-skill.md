### Virtual Threads on Endpoints

- Annotate REST endpoints with `@RunOnVirtualThread` to run on virtual threads.
- This allows blocking code (JDBC, file I/O) without consuming platform threads.

### When to Use

- Use for blocking I/O operations (database, file, external APIs).
- Do NOT use for CPU-intensive work — virtual threads don't help there.
- Do NOT use if your code is already reactive (Mutiny/Uni).

### Testing

- Use `@QuarkusTest` — virtual threads are transparent to tests.

### Common Pitfalls

- Requires Java 21+.
- Avoid `synchronized` blocks — they pin the carrier thread. Use `ReentrantLock` instead.
- Do NOT mix `@RunOnVirtualThread` with reactive return types (`Uni`, `Multi`).
