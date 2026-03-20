### Auto-Instrumentation

- REST endpoints, JDBC, gRPC, and messaging are automatically traced — no code needed.
- Traces include HTTP method, path, status code, and latency.

### Custom Spans

- Use `@WithSpan` annotation on CDI bean methods to create custom spans.
- Inject `Tracer` or `Span` for programmatic span creation.
- Add attributes with `Span.current().setAttribute("key", "value")`.

### Configuration

- `quarkus.otel.exporter.otlp.endpoint` — OTLP exporter endpoint (e.g. Jaeger, Tempo).
- `quarkus.otel.resource.attributes=service.name=my-service` — set the service name.
- `quarkus.otel.enabled=false` to disable in tests.

### Testing

- Disable tracing in tests: `quarkus.otel.enabled=false` or use a no-op exporter.
- For trace verification, use the in-memory exporter.

### Common Pitfalls

- Do NOT create high-cardinality span attributes (e.g. user IDs as span names).
- `@WithSpan` only works on CDI bean methods.
