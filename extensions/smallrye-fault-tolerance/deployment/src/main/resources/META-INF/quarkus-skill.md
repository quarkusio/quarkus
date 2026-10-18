
## Fault Tolerance Strategies

Apply MicroProfile Fault Tolerance annotations to **CDI bean** methods. Core annotations are in `org.eclipse.microprofile.faulttolerance.*`. SmallRye extensions (like `@RateLimit`) are in `io.smallrye.faulttolerance.api.*`.

### @Retry — retry on failure

```java
@Retry(maxRetries = 3, delay = 500)
public String callService(String id) { ... }

// Selective retry
@Retry(maxRetries = 3,
       retryOn = {ConnectException.class, TimeoutException.class},
       abortOn = {InvalidInputException.class})
public String callService(String input) { ... }
```

- `retryOn` — only retry on these exceptions (and subclasses)
- `abortOn` — stop retrying immediately on these exceptions (fallback still applies)

### @Timeout — fail if too slow

```java
@Timeout(2000) // 2 seconds (default unit is milliseconds)
public String fetchData() { ... }
```

Use `unit = ChronoUnit.SECONDS` for clarity: `@Timeout(value = 2, unit = ChronoUnit.SECONDS)`.

### @CircuitBreaker — stop calling a failing service

```java
@CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5000)
public String callService() { ... }
```

Opens the circuit after `failureRatio` failures in `requestVolumeThreshold` calls. While open, calls fail immediately with `CircuitBreakerOpenException`. After `delay` ms, the circuit half-opens to probe.

### @Fallback — provide an alternative

```java
@Retry(maxRetries = 3)
@Fallback(fallbackMethod = "fallbackGetData")
public String getData(String id) { ... }

private String fallbackGetData(String id) {
    return "cached-" + id;
}
```

Fallback method must have the **same parameters and return type**. It can be private.

### @Bulkhead — limit concurrency

```java
@Bulkhead(5)
public String limitedCall() { ... }
```

Excess calls throw `BulkheadException`. For async methods, also supports a waiting queue: `@Bulkhead(value = 5, waitingTaskQueue = 10)`.

### @RateLimit — throttle call rate (SmallRye extension)

```java
import io.smallrye.faulttolerance.api.RateLimit;

@RateLimit(value = 50, window = 1, windowUnit = ChronoUnit.MINUTES)
public String rateLimited() { ... }
```

Limits to 50 calls per minute. Excess calls throw `RateLimitException`.

### Combining annotations

Multiple annotations form a call chain (outermost to innermost):

**Fallback → Retry → CircuitBreaker → RateLimit → Timeout → Bulkhead → method call**

Retry re-attempts the entire inner chain. Fallback catches any exception that survives all retries. The order annotations appear in source doesn't matter — the chain order is fixed.

### Async fault tolerance

Methods returning `CompletionStage` or `Uni` automatically get async fault tolerance — **no `@Asynchronous` annotation needed** (Quarkus enables non-compatible mode by default):

```java
@Retry(maxRetries = 3)
@Fallback(fallbackMethod = "asyncFallback")
public Uni<String> asyncCall(String id) { ... }
```

## Configuration override

Override annotation values via `application.properties`:

```properties
quarkus.fault-tolerance."com.example.MyService/myMethod".retry.max-retries=5
quarkus.fault-tolerance."com.example.MyService/myMethod".circuit-breaker.request-volume-threshold=5
quarkus.fault-tolerance."com.example.MyService/myMethod".timeout.value=3000
```

## Testing

```java
@QuarkusTest
class FaultToleranceTest {
    @Inject MyService service;

    @Test
    void testFallbackOnFailure() {
        String result = service.getData("bad-id");
        assertEquals("cached-bad-id", result);
    }
}
```

For circuit breaker tests, use small `requestVolumeThreshold` and `delay` values. Circuit breaker state persists across test methods — use separate test classes or account for shared state.

## Common Pitfalls

- **All time values default to milliseconds.** `@Timeout(2000)` = 2 seconds. Use `unit = ChronoUnit.SECONDS` for clarity.
- **Annotations only work on CDI beans.** Creating the object with `new` bypasses fault tolerance entirely.
- **`abortOn` doesn't prevent fallback.** It only skips retries — the Fallback layer (outermost) still catches the exception.
- **CircuitBreaker defaults are generous.** `requestVolumeThreshold` = 20, `failureRatio` = 0.5, `delay` = 5000ms. Use smaller values in tests.
- **`@RateLimit` is a SmallRye extension** — import from `io.smallrye.faulttolerance.api`, not the MicroProfile package.
