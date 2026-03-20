### Usage

- Add this extension for Spring `@Scheduled` annotation compatibility in Quarkus.
- Use `@Scheduled(cron = "...")` or `@Scheduled(fixedRate = 5000)` on CDI bean methods.

### Pattern

```java
@ApplicationScoped
public class MyScheduler {
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyTask() { ... }

    @Scheduled(fixedRate = 5000)
    public void every5Seconds() { ... }
}
```

### Testing

- Use `@QuarkusTest` — scheduled methods execute during tests.
- Consider disabling schedulers in tests if they interfere: `quarkus.scheduler.enabled=false`.

### Common Pitfalls

- For new Quarkus applications, prefer `quarkus-scheduler` with `@io.quarkus.scheduler.Scheduled` — it supports more features.
- This extension is a compatibility layer — not all Spring Scheduled features are supported.
