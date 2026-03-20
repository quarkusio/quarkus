### Usage

- Add this extension alongside `quarkus-micrometer` to expose metrics in Prometheus format.
- Metrics are available at `/q/metrics` in Prometheus text format.
- HTTP, JVM, and system metrics are collected automatically.

### Custom Metrics

- Inject `MeterRegistry` to create custom counters, gauges, and timers.
- See the `quarkus-micrometer` skill for custom metrics patterns.

### Testing

- Use `@QuarkusTest` with REST Assured — fetch `/q/metrics` and assert on metric names and values.

### Common Pitfalls

- This extension only provides the Prometheus registry — you still need `quarkus-micrometer` for the core Micrometer API.
- Do NOT add multiple registry extensions unless you want to export to multiple backends.
