
### When to Use

Use `quartz` instead of `scheduler` when you need: JDBC-backed persistent jobs, clustering, the direct Quartz API for programmatic job management, or misfire handling. For simple in-memory scheduling, `quarkus-scheduler` alone is sufficient.

### Scheduled Methods

```java
@ApplicationScoped
public class Jobs {
    @Scheduled(every = "10s", identity = "my-job")
    void everyTenSeconds() { /* runs every 10 seconds */ }

    @Scheduled(cron = "0 0 8 * * ?", identity = "morning-report")
    void dailyAt8am() { /* Quartz cron format (6 fields, includes seconds) */ }
}
```

- Methods must be on CDI beans, return `void` or `Uni<Void>`, and take no arguments (or `ScheduledExecution`).
- The `identity` is used for pause/resume operations and must be unique.
- Quartz cron uses **6 fields** by default (seconds minutes hours day-of-month month day-of-week). This is configurable via `quarkus.scheduler.cron-type` — see the [scheduler reference](https://quarkus.io/guides/scheduler-reference#quarkus-scheduler_quarkus-scheduler-cron-type).

### Scheduler APIs

1. **`io.quarkus.scheduler.Scheduler`** — Quarkus abstraction for controlling scheduled jobs:
   ```java
   @Inject Scheduler scheduler;
   scheduler.pause("my-job");
   scheduler.resume("my-job");
   boolean running = scheduler.isRunning();
   ```

2. **`io.quarkus.quartz.QuartzScheduler`** — Quarkus-native way to access Quartz features, including programmatic job creation and direct access to the underlying Quartz `Scheduler`:
   ```java
   @Inject QuartzScheduler quartzScheduler;

   // Programmatic job via Quarkus API
   quartzScheduler.newJob("dynamic-job")
       .setInterval("10s")
       .setTask(execution -> { /* ... */ })
       .schedule();

   // Or access the underlying org.quartz.Scheduler directly
   org.quartz.Scheduler quartz = quartzScheduler.getScheduler();
   ```

Use the Quarkus `Scheduler` for pause/resume and status. Use `QuartzScheduler` for programmatic job management and Quartz-specific features.

### Programmatic Jobs (Quartz API)

Implement `org.quartz.Job`:

```java
public class MyJob implements Job {
    @Inject SomeService service;  // CDI injection works

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String param = context.getJobDetail().getJobDataMap().getString("param");
        service.process(param);
    }
}
```

- CDI injection works in `Job` classes automatically.
- Use `@DisallowConcurrentExecution` to prevent overlapping executions.
- Access job data via `context.getJobDetail().getJobDataMap()`.

### Concurrent Execution

- By default, `@Scheduled` methods CAN run concurrently with themselves.
- Use `@Nonconcurrent` on a `@Scheduled` method to prevent overlapping executions.
- Alternatively, use `@Scheduled(concurrentExecution = SKIP)`.
- For Quartz `Job` classes, use `@DisallowConcurrentExecution`.

### JDBC Job Store (Persistent Jobs)

```properties
quarkus.quartz.store-type=jdbc-cmt
quarkus.quartz.clustered=true
```

Requires a datasource. Quartz tables must be created manually via SQL migration (Flyway or Liquibase) — see the [Quarkus Quartz guide](https://quarkus.io/guides/quartz#creating-quartz-tables) for the required table definitions. Jobs survive application restarts.

### Testing

- `@Scheduled` methods run during tests by default.
- Disable scheduling in tests if not needed: `quarkus.scheduler.enabled=false`.
- For timing-based tests, use `Awaitility`:
  ```java
  await().atMost(Duration.ofSeconds(5)).until(() -> counter.get() > 0);
  ```
- After pausing a job, wait and verify the counter doesn't change.

### Misfire Handling

Misfires occur when a job's trigger time passes without the job executing (e.g., the application was down). Configure misfire policies on `@Scheduled`:

```java
@Scheduled(cron = "0 0 8 * * ?", identity = "daily-report",
    misfirePolicy = MisfirePolicy.FIRE_NOW)
void dailyReport() { /* ... */ }
```

- `SMART_POLICY` (default) — Quartz picks a sensible policy based on the trigger type.
- `FIRE_NOW` — execute immediately on recovery.
- `IGNORE_MISFIRE_POLICY` — fire all missed triggers.
- `DO_NOTHING` — skip misfired triggers and wait for the next scheduled time.

### Common Pitfalls

- Quartz cron uses **6 fields** by default (with seconds) — `"0 */5 * * * ?"` not `"*/5 * * * *"`.
- `@Scheduled` methods must be on CDI beans — plain classes are ignored.
- Do NOT use `@Scheduled` on private methods.
- The Quarkus `Scheduler` and Quartz `Scheduler` are different types — inject the right one for your use case.
