### Usage

- This extension provides Eclipse Vert.x integration — the reactive core underlying Quarkus.
- Inject `Vertx` or `io.vertx.mutiny.core.Vertx` for direct Vert.x API access.
- Use the event bus with `vertx.eventBus()` for in-app messaging.

### Event Bus

- Send messages: `vertx.eventBus().send("address", message)`.
- Consume messages: `@ConsumeEvent("address")` on a CDI bean method.
- Event bus communication is in-process only (not distributed).

### Timers and Periodic Tasks

- Use `vertx.setTimer(delay, handler)` for one-shot timers.
- Use `vertx.setPeriodic(interval, handler)` for recurring tasks.
- For scheduled tasks, prefer `quarkus-scheduler` for a simpler API.

### Testing

- Use `@QuarkusTest` — Vert.x is started automatically.
- Inject `Vertx` in tests for direct access.

### Common Pitfalls

- Do NOT block the Vert.x event loop — use `@Blocking` annotation or `executeBlocking()` for blocking operations.
- Prefer the Mutiny variant (`io.vertx.mutiny.core.Vertx`) over the bare Vert.x API for better integration with Quarkus reactive programming.
