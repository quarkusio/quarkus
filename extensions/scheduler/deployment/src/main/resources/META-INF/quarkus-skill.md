### Scheduled Methods

- `@Scheduled(every = "10s")` — run every 10 seconds.
- `@Scheduled(cron = "0 0 * * * ?")` — cron expression (every hour).
- `@Scheduled(delayed = "5s")` — initial delay before first execution.
- Methods must return `void` or `Uni<Void>` and take no arguments (or `ScheduledExecution`).

### Programmatic Scheduling

- Inject `Scheduler` to pause, resume, or create jobs programmatically.
- Use `scheduler.newJob("my-job").setInterval("10s").setTask(ctx -> ...).schedule()`.

### Concurrent Execution

- By default, a scheduled method CAN run concurrently with itself.
- Use `@Scheduled(concurrentExecution = SKIP)` to prevent concurrent runs.

### Testing

- Use `@QuarkusTest` — scheduled methods run during tests.
- Disable scheduling in tests if not needed: `quarkus.scheduler.enabled=false`.

### Common Pitfalls

- Scheduled methods must be on CDI beans — plain classes are ignored.
- Do NOT use `@Scheduled` on private methods.
