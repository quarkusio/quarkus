### Auto-Instrumentation

- HTTP server/client, gRPC, and messaging metrics are collected automatically.
- No code changes needed for basic metrics.

### Custom Metrics

- Inject `MeterRegistry` and create metrics: counters, gauges, timers, distribution summaries.
- Use `@Counted` and `@Timed` annotations on CDI bean methods for declarative metrics.
- Name metrics with dots: `my.app.orders.count`.

### Prometheus Endpoint

- Add `quarkus-micrometer-registry-prometheus` for Prometheus-format metrics at `/q/metrics`.
- This is the most common setup for Kubernetes monitoring.

### Configuration

- `quarkus.micrometer.binder.http-server.enabled=true` (default) — HTTP server metrics.
- `quarkus.micrometer.export.prometheus.enabled=true` (default with registry) — Prometheus export.
- Tag customization: implement `HttpServerMetricsTagsContributor`.

### Testing

- Inject `MeterRegistry` in tests and assert metric values.
- Use `registry.get("http.server.requests").timer()` to verify auto-collected metrics.

### Common Pitfalls

- Do NOT create metrics with high cardinality tags (e.g. user IDs) — this causes memory issues.
- The `/q/metrics` endpoint requires the Prometheus registry extension, not just Micrometer core.
