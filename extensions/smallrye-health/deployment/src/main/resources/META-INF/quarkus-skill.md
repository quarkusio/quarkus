### Health Check Types

- `@Liveness` — is the app alive? If not, the platform should restart it.
- `@Readiness` — is the app ready to serve traffic? If not, stop sending requests.
- `@Startup` — has the app started? Used for slow-starting apps.

### Implementing Health Checks

- Implement `HealthCheck` interface and annotate with `@Liveness`, `@Readiness`, or `@Startup`.
- Return `HealthCheckResponse.up("name")` or `HealthCheckResponse.down("name")`.
- Use `.withData("key", "value")` to add diagnostic information.

### Endpoints

- `/q/health/live` — liveness checks.
- `/q/health/ready` — readiness checks.
- `/q/health/started` — startup checks.
- `/q/health` — all checks combined.

### Automatic Health Checks

- Extensions auto-register health checks (e.g. datasource, Kafka, Redis).
- Disable with `quarkus.health.extensions.enabled=false`.

### Testing

- Use REST Assured to call `/q/health/live` or `/q/health/ready` and assert status 200.

### Common Pitfalls

- Do NOT do heavy work in health checks — they are called frequently by orchestrators.
- Liveness checks should NOT depend on external services — only check if the app itself is alive.
