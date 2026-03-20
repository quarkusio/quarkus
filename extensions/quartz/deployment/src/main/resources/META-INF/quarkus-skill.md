### Quartz Scheduling

- Use `@Scheduled` — same as the Scheduler extension but backed by Quartz.
- Quartz adds: clustered scheduling, persistent jobs, and misfire handling.

### Clustered Mode

- Set `quarkus.quartz.clustered=true` for cluster-safe scheduling.
- Requires a JDBC store: `quarkus.quartz.store-type=jdbc-cmt`.
- Database tables are auto-created with `quarkus.quartz.table-prefix=QRTZ_`.

### Testing

- Use `@QuarkusTest` — Quartz runs during tests.
- Disable for fast tests: `quarkus.scheduler.enabled=false`.

### Common Pitfalls

- Clustered mode requires a shared database — all nodes must use the same datasource.
- Do NOT use in-memory store for clustered deployments.
