### Fault Tolerance Annotations

- `@Retry(maxRetries = 3)` — retry on failure.
- `@Timeout(value = 5, unit = ChronoUnit.SECONDS)` — timeout.
- `@CircuitBreaker(requestVolumeThreshold = 10)` — circuit breaker pattern.
- `@Bulkhead(value = 5)` — limit concurrent executions.
- `@Fallback(fallbackMethod = "fallbackMethod")` — fallback on failure.

### Combining Annotations

- Annotations can be combined on the same method.
- Order: Bulkhead -> Timeout -> CircuitBreaker -> Retry -> Fallback.

### Configuration Override

- Override via config: `com.example.MyService/myMethod/Retry/maxRetries=5`.
- Global override: `Retry/maxRetries=5`.

### Testing

- Use `@QuarkusTest` — fault tolerance is active.
- Test fallback paths by injecting failures.

### Common Pitfalls

- Annotations only work on CDI bean methods.
- `@Timeout` does NOT interrupt the thread — it throws `TimeoutException`.
