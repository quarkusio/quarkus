
### Key Patterns when instrumenting applications with OpenTelemetry

- Tracing is enabled by default with zero code changes. Do NOT use the OpenTelemetry Java agent — Quarkus provides its own instrumentation and the agent does not work in native mode.
- Auto-instrumented: REST (server + client), gRPC, JDBC, Redis, MongoDB, Kafka, AMQP, RabbitMQ, Pulsar, Scheduler, GraphQL, Vert.x HTTP.
- Metrics require `quarkus.otel.metrics.enabled=true` (build-time — requires rebuild).
- Logs require `quarkus.otel.logs.enabled=true` (build-time — requires rebuild).
- JDBC telemetry is opt-in: `quarkus.datasource.jdbc.telemetry=true`.

### Tracing with Annotations

Use annotations from `io.opentelemetry.instrumentation.annotations`:

- `@WithSpan` — creates a new span wrapping the method invocation.
- `@WithSpan("custom-name")` — span with an explicit name. Setting a custom `SpanKind` is also available.
- `@SpanAttribute("key")` — marks a method parameter as a span attribute.
- `@AddingSpanAttributes` — adds parameter attributes to the current span without creating a new one.

```java
@ApplicationScoped
public class OrderService {
    @WithSpan("process-order")
    public Order process(@SpanAttribute("order.id") String orderId) {
        return doProcess(orderId);
    }
    @AddingSpanAttributes
    public void enrich(@SpanAttribute("order.priority") String priority) {
        // no new span — "order.priority" is added to the current span
    }
}
```

Annotations work with `Uni<T>` and `Multi<T>` return types. They only work on CDI bean methods — not on instances created with `new` or on private methods.

### Manual Span Creation

Inject `Tracer` via CDI. Always call `span.end()` in a `finally` block and use `span.makeCurrent()` so downstream calls inherit the context:

```java
@Inject
Tracer tracer;

public void charge(String paymentId) {
    Span span = tracer.spanBuilder("charge-payment")
            .setAttribute("payment.id", paymentId)
            .setSpanKind(SpanKind.CLIENT)
            .startSpan();
    try (Scope scope = span.makeCurrent()) {
        processPayment(paymentId);
    } catch (Exception e) {
        span.setStatus(StatusCode.ERROR, e.getMessage());
        span.recordException(e);
        throw e;
    } finally {
        span.end();
    }
}
```

### Sampling

- Default sampler: `parentbased_always_on` (all traces are sampled).
- In production, use between 1% and 10% sampling:
  ```properties
  quarkus.otel.traces.sampler=parentbased_traceidratio
  quarkus.otel.traces.sampler.arg=0.1
  ```
- Suppress traces for specific URIs: `quarkus.otel.traces.suppress-application-uris=health,ready,metrics`.
- For custom sampling logic, create a CDI producer returning `io.opentelemetry.sdk.trace.samplers.Sampler`.

### Metrics

Enable metrics (build-time property — requires rebuild):
```properties
quarkus.otel.metrics.enabled=true
```

Inject `Meter` via CDI to create instruments:

```java
@ApplicationScoped
public class InventoryService {
    @Inject
    Meter meter;
    void onItemSold(String category) {
        meter.counterBuilder("items.sold")
                .setDescription("Number of items sold")
                .build()
                .add(1, Attributes.of(AttributeKey.stringKey("category"), category));
    }
}
```

Available instrument types: `LongCounter`, `DoubleCounter`, `LongUpDownCounter`, `DoubleHistogram`, `DoubleGauge`, and observable variants (`ObservableLongCounter`, etc.).

Avoid unbounded attribute values (e.g., userId, requestId) — they cause cardinality explosion and high memory usage.

For annotation-based metrics (`@Timed`, `@Counted`), use the `quarkus-micrometer-opentelemetry` bridge instead.

### Logging

Enable log export (build-time property — requires rebuild):
```properties
quarkus.otel.logs.enabled=true
```

Quarkus automatically bridges JBoss LogManager to the OpenTelemetry Logs SDK. Existing `Logger.info()` calls are exported as OTel log records.

### Testing

Use `InMemorySpanExporter` from `opentelemetry-sdk-testing` as a CDI bean to capture and assert spans in tests:

```java
@ApplicationScoped
public class InMemorySpanExporterProducer {
    @Produces
    @Singleton
    InMemorySpanExporter inMemorySpanExporter() {
        return InMemorySpanExporter.create();
    }
}
```

```java
@QuarkusTest
class OrderResourceTest {
    @Inject
    InMemorySpanExporter spanExporter;

    @BeforeEach
    void reset() {
        spanExporter.reset();
    }

    @Test
    void testOrderSpan() {
        given().when().get("/orders/123").then().statusCode(200);
        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            assertThat(spans).anyMatch(s -> s.getName().equals("GET /orders/{id}"));
        });
    }
}
```
The tests use `rest-assured` and `awatility` as dependencies. 

If @QuarkusIntegrationTest is required, because of native, a `ExporterResource` REST endpoint wrapping the access to InMemorySpanExporter is needed because the bean will be located in a separate process.

### Common Pitfalls

- **Use default protocol.** Unless instructed otherwise, use the default `grpc`.
- **Protocol and port must match.** Default `grpc` uses port 4317, `http/protobuf` uses port 4318. A mismatch causes silent export failures.
- **Build-time vs runtime.** `quarkus.otel.exporter.otlp.endpoint` is a runtime property. `quarkus.otel.traces.enabled` is build-time. Changing a build-time property at runtime has no effect.
- **Serverless requires simple export.** Set `quarkus.otel.simple=true` for immediate export instead of batching — batched spans may be lost when the function terminates.
- **Disable specific instrumentation** with `quarkus.otel.instrument.<extension>=false` (e.g., `quarkus.otel.instrument.rest=false`).
- **Local dev visualization.** Add `quarkus-observability-devservices-lgtm` (scope `provided`) to auto-start Grafana, Tempo, Prometheus, and an OTel Collector in dev/test mode.
- **Avoid adding default configurations.** No need to add configurations to `application.properties` if the required values are already default.
- **Spans arrive asynchronously to the exporter.** Use `Awaitility` or polling, not immediate assertions.
