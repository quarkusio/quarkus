### REST on Lambda

- Write standard REST endpoints with `@Path` — they work as Lambda functions automatically.
- API Gateway routes HTTP requests to the Lambda function.

### Configuration

- No special config needed — existing REST endpoints are automatically Lambda-compatible.

### Testing

- Use `@QuarkusTest` with REST Assured — tests run locally against the HTTP layer.

### Common Pitfalls

- Cold start latency is significant on JVM — consider native image for Lambda.
- WebSocket and SSE are NOT supported on Lambda.
