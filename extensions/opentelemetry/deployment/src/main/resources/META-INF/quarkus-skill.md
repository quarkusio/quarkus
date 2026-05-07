
### Automatic Instrumentation

- REST endpoints, REST clients, gRPC, and reactive messaging are traced automatically — no code changes needed.
- The service name defaults to `quarkus.application.name`. Override with `quarkus.otel.service.name` if needed.
- OTLP exporter sends to `http://localhost:4317` by default. Change with `quarkus.otel.exporter.otlp.endpoint`.

### @WithSpan and @SpanAttribute

No extra dependency needed — `@WithSpan` and `@SpanAttribute` are on the classpath when you add `quarkus-opentelemetry`.

```java
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;

@WithSpan("validate-order")
public void validate(@SpanAttribute("order.item") String item) {
    // creates a span named "validate-order" with attribute "order.item"
}
```

- Default span name is `ClassName.methodName` — override with `@WithSpan("custom-name")`.
- Set span kind: `@WithSpan(kind = SpanKind.CLIENT)`. Default is `INTERNAL`.
- Methods must be on CDI beans — `@WithSpan` on plain classes is ignored.

### Manual Span Creation

Inject `Tracer` to create spans programmatically:

```java
@Inject Tracer tracer;

public void process() {
    Span span = tracer.spanBuilder("process-step")
        .setAttribute("step.id", stepId)
        .setParent(Context.current().with(Span.current()))
        .startSpan();
    try (Scope scope = span.makeCurrent()) {
        // work here — child spans will link to this parent
    } catch (Exception e) {
        span.setStatus(StatusCode.ERROR);
        span.recordException(e);
        throw e;
    } finally {
        span.end();
    }
```

### Available CDI Injections

- `io.opentelemetry.api.OpenTelemetry` — the SDK entry point
- `io.opentelemetry.api.trace.Tracer` — for creating spans
- `io.opentelemetry.api.trace.Span` — the current active span
- `io.opentelemetry.api.baggage.Baggage` — for cross-service context propagation

### Span Events and Attributes

```java
Span.current().addEvent("cache-miss");
Span.current().setAttribute("order.total", 42.0);
Span.current().recordException(exception);
Span.current().setStatus(StatusCode.ERROR, "validation failed");
```

### Baggage Propagation

```java
Baggage baggage = Baggage.current().toBuilder().put("correlation.id", value).build();
try (Scope scope = Context.current().with(baggage).makeCurrent()) {
    // REST Client calls here automatically propagate baggage
}
// In downstream service: Baggage.current().getEntryValue("correlation.id")
```

### Metrics

Metrics are disabled by default. Enable with `quarkus.otel.metrics.enabled=true`.
Inject `Meter` to create counters and histograms:

```java
@Inject Meter meter;
LongCounter counter = meter.counterBuilder("requests.count").build();
counter.add(1, Attributes.of(AttributeKey.stringKey("endpoint"), "/api/foo"));
```

### Testing

Use the SPI-based `ConfigurableSpanExporterProvider` pattern to capture spans in tests:

1. Create a `TestSpanExporter` CDI bean implementing `SpanExporter`:
   ```java
   @ApplicationScoped
   public class TestSpanExporter implements SpanExporter {
       private final List<SpanData> spans = new CopyOnWriteArrayList<>();
       public List<SpanData> getFinishedSpans() { return spans; }
       public void reset() { spans.clear(); }
       @Override public CompletableResultCode export(Collection<SpanData> spans) {
           this.spans.addAll(spans); return CompletableResultCode.ofSuccess();
       }
       @Override public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
       @Override public CompletableResultCode shutdown() { spans.clear(); return CompletableResultCode.ofSuccess(); }
   }
   ```
2. Create a `TestSpanExporterProvider` implementing `ConfigurableSpanExporterProvider`:
   ```java
   public class TestSpanExporterProvider implements ConfigurableSpanExporterProvider {
       @Override public SpanExporter createExporter(ConfigProperties config) {
           return CDI.current().select(TestSpanExporter.class).get();
       }
       @Override public String getName() { return "test-span-exporter"; }
   }
   ```
3. Register via SPI: create `META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider` containing the provider class name.
4. Set `quarkus.otel.traces.exporter=test-span-exporter` and `quarkus.otel.bsp.schedule.delay=50ms` in test `application.properties`.
5. Inject `TestSpanExporter`, call the endpoint, then use Awaitility to wait for spans (they arrive asynchronously).

### Dev Services

Add `quarkus-observability-devservices-lgtm` for a Grafana+Tempo+Loki+Prometheus stack in dev mode. This is a separate extension — not included by default.

### Customization

- Produce a `Resource` CDI bean to add custom attributes to all spans (e.g., environment, version).
- Implement `SpanProcessor` as a CDI bean to modify spans at start/end (e.g., adding global attributes).

### Common Pitfalls

- `@WithSpan` only works on CDI bean methods — plain class methods are not intercepted.
- Spans arrive asynchronously to the exporter — use `Awaitility` or polling, not immediate assertions.
- Always call `span.end()` in a `finally` block — unclosed spans leak resources.
- `Span.current()` returns a no-op span if no trace context is active (e.g., outside a request).
- `io.opentelemetry.semconv.ResourceAttributes` is NOT available — use `AttributeKey.stringKey()` instead.
