
## Key Patterns

### Creating health checks

Implement `HealthCheck` and annotate with `@Liveness`, `@Readiness`, or `@Startup`:

```java
@Liveness
@ApplicationScoped
public class MyLivenessCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("my-liveness");
    }
}
```

- **`@Liveness`** — is the application running? (Kubernetes restarts on failure)
- **`@Readiness`** — can the application handle requests? (Kubernetes stops routing traffic on failure)
- **`@Startup`** — has the application finished initializing? (Kubernetes kills on failure during startup)

All annotations are in `org.eclipse.microprofile.health.*`. Watch for the clash with `io.quarkus.runtime.Startup` — use the full import for the health annotation.

### Response builder

Use the fluent builder for responses with custom data:

```java
@Override
public HealthCheckResponse call() {
    return HealthCheckResponse.named("Database Connection")
        .up()
        .withData("connection", "established")
        .withData("pool-size", 10)
        .build();
}
```

Shortcuts: `HealthCheckResponse.up("name")` and `HealthCheckResponse.down("name")` for simple cases.

### Startup check with initialization tracking

Use `@Observes StartupEvent` to track when initialization completes:

```java
@Startup
@ApplicationScoped
public class InitHealthCheck implements HealthCheck {
    private volatile boolean ready = false;

    void onStart(@Observes StartupEvent event) {
        // perform initialization
        ready = true;
    }

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("init")
            .status(ready)
            .build();
    }
}
```

### Reactive health checks

For non-blocking checks, implement `AsyncHealthCheck` (SmallRye-specific):

```java
@Readiness
@ApplicationScoped
public class ReactiveCheck implements AsyncHealthCheck {
    @Override
    public Uni<HealthCheckResponse> call() {
        return Uni.createFrom().item(HealthCheckResponse.up("reactive-check"));
    }
}
```

Import: `io.smallrye.health.api.AsyncHealthCheck`.

### Health groups

Group checks into custom categories with `@HealthGroup` (SmallRye-specific):

```java
@HealthGroup("external")
@ApplicationScoped
public class ExternalApiCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("external-api");
    }
}
```

Access grouped checks at `/q/health/group/external`. Import: `io.smallrye.health.api.HealthGroup`. A check can belong to both a group and a standard category (e.g., `@Readiness @HealthGroup("external")`).

## Common Pitfalls

- **`@Startup` import clash.** `org.eclipse.microprofile.health.Startup` vs `io.quarkus.runtime.Startup` — use the health one for health checks.
- **Health checks must be CDI beans.** Always add `@ApplicationScoped` (or another scope) alongside the health annotation.
- **DOWN status cascades.** If any check in a group reports DOWN, the overall endpoint returns DOWN with HTTP 503.
- **Default endpoints are under `/q/`.** Liveness at `/q/health/live`, readiness at `/q/health/ready`, startup at `/q/health/started`, combined at `/q/health`.

## Testing

Test health endpoints with REST-assured against the JSON response:

```java
@QuarkusTest
class HealthCheckTest {

    @Test
    void testLiveness() {
        given()
            .when().get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", is("UP"))
            .body("checks.find { it.name == 'my-liveness' }.status", is("UP"));
    }

    @Test
    void testReadinessWithData() {
        given()
            .when().get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("checks.find { it.name == 'Database Connection' }.data.connection",
                  is("established"));
    }
}
```

Health response JSON structure:
```json
{"status": "UP", "checks": [{"name": "...", "status": "UP", "data": {...}}]}
```

When any check is DOWN, the HTTP status changes to **503** and `"status"` becomes `"DOWN"`.
