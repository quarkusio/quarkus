
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
- Quartz cron uses **6 fields** (seconds minutes hours day-of-month month day-of-week), not the standard 5-field Unix cron.

### Two Scheduler APIs

1. **`io.quarkus.scheduler.Scheduler`** — Quarkus abstraction for controlling scheduled jobs:
   ```java
   @Inject Scheduler scheduler;
   scheduler.pause("my-job");
   scheduler.resume("my-job");
   boolean running = scheduler.isRunning();
   ```

2. **`org.quartz.Scheduler`** — Direct Quartz API for programmatic job creation:
   ```java
   @Inject org.quartz.Scheduler quartz;

   JobDetail job = JobBuilder.newJob(MyJob.class)
       .withIdentity("dynamic-job", "my-group")
       .usingJobData("param", "value")
       .build();
   Trigger trigger = TriggerBuilder.newTrigger()
       .withIdentity("dynamic-trigger", "my-group")
       .withSchedule(SimpleScheduleBuilder.simpleSchedule()
           .withIntervalInSeconds(5).repeatForever())
       .build();
   quartz.scheduleJob(job, trigger);
   ```

Use the Quarkus `Scheduler` for pause/resume and status. Use the Quartz `Scheduler` for creating/deleting jobs at runtime.

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
- Use `@Scheduled(concurrentExecution = SKIP)` to prevent concurrent runs.
- For Quartz `Job` classes, use `@DisallowConcurrentExecution`.

### JDBC Job Store (Persistent Jobs)

```properties
quarkus.quartz.store-type=jdbc-cmt
quarkus.quartz.clustered=true
```

Requires a datasource. Quartz tables are created automatically if `quarkus.quartz.store-type=jdbc-cmt` is set. Jobs survive application restarts.

### Testing

- `@Scheduled` methods run during tests by default.
- Disable scheduling in tests if not needed: `quarkus.scheduler.enabled=false`.
- For timing-based tests, use `Awaitility`:
  ```java
  await().atMost(Duration.ofSeconds(5)).until(() -> counter.get() > 0);
  ```
- After pausing a job, wait and verify the counter doesn't change.

### Common Pitfalls

- Quartz cron uses **6 fields** (with seconds) — `"0 */5 * * * ?"` not `"*/5 * * * *"`.
- `@Scheduled` methods must be on CDI beans — plain classes are ignored.
- Do NOT use `@Scheduled` on private methods.
- The Quarkus `Scheduler` and Quartz `Scheduler` are different types — inject the right one for your use case.
- "No scheduled business methods found" log message at startup is normal if code hasn't compiled yet in dev mode.
