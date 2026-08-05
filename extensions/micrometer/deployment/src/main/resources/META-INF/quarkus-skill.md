
### Instrument a Quarkus application with Micrometer - Getting Started

- Add `quarkus-micrometer-registry-prometheus` for Prometheus metrics export via `/q/metrics`.
- Do NOT add `quarkus-opentelemetry` unless you also need distributed tracing. Micrometer and OpenTelemetry are independent; mixing them without a reason only adds complexity. If you need both Micrometer APIs and OpenTelemetry export, use `quarkus-micrometer-opentelemetry` — it bridges Micrometer metrics to the OpenTelemetry SDK.
- For local observability (Grafana dashboards), add the LGTM dev service. Do NOT hardcode exporter endpoints. This dev service detects the `quarkus-micrometer-registry-prometheus` extension and
  activates a scraper for it. Do NOT use the generic `quarkus-observability-devservices` — it does not include the LGTM stack. Use:

```xml
<dependency>                                                                                                                                                                                           
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-observability-devservices-lgtm</artifactId>
    <scope>provided</scope>                                                                                                                                                                            
</dependency>
```

### Programmatic Metrics — Use MeterProvider

Inject `MeterRegistry` via constructor. Create metrics with the builder pattern and `.withRegistry(registry)` to get a `MeterProvider`. This defers tag binding to the call site, so you define the metric shape once and add dynamic tags when recording:

```java
@Path("/orders")
public class OrderResource {
    private final io.micrometer.core.instrument.Meter.MeterProvider<Counter> orderCounter;
    public OrderResource(MeterRegistry registry) {
        orderCounter = Counter.builder("orders.placed")
                .description("Total orders placed")
                .withRegistry(registry);
    }
    @POST
    public Response placeOrder(Order order) {
        orderCounter.withTags(Tags.of("type", order.getType())).increment();
        return processOrder(order);
    }
}
```

Use `.withRegistry(registry)` instead of `.register(registry)` whenever possible — it defers tag binding to the call site via `.withTags()`, so you define the metric once instead of creating one meter per tag combination upfront.
Available `MeterProvider` types: `Counter`, `Timer`, `LongTaskTimer`, `DistributionSummary`.
A timer wraps the actual call. A distribution summary records the actual business value. A counter increments on the actual event. Do not wrap synthetic work — if removing the metric would leave an empty method, the metric has nothing real to measure. Example for `@Timed`:

```java                                
// BAD — Thread.sleep is not real work
@Timed("process.time")
public void process() {
  Thread.sleep(100);
}
// GOOD — timer wraps actual business logic                                                                           
@Timed("process.time")
public void process(Order order) {
  orderService.validate(order);
  orderService.save(order);
}         
```

### Gauges — Must Reflect Live State

A gauge reports the current value of something that can go up AND down (active connections, queue depth, cache size). If your value only increments, use a counter instead.
When the state already lives in an object (a queue, a pool, a cache), pass that object and a lambda — don't duplicate the state into a separate counter:

```java
@ApplicationScoped
public class TaskQueue {
    private final ConcurrentLinkedQueue<Task> queue = new ConcurrentLinkedQueue<>();
    TaskQueue(MeterRegistry registry) {
        Gauge.builder("queue.depth", queue, q -> (double) q.size())
                .description("Current number of tasks in the queue")
                .register(registry);
    }
    public void enqueue(Task task) { queue.add(task); }
    public Task poll() { return queue.poll(); }
}
```

When no existing object holds the value (e.g., in-flight request counting), use `AtomicInteger` with `Gauge.builder("name", atomicInt, AtomicInteger::get)` and expose both `increment()` and `decrement()` - call `increment()` when a request starts and `decrement()` when it finishes.
Add `@Startup` to the class if no other bean injects the gauge holder. If the bean is already injected elsewhere, CDI creates it on first use and the gauge registers automatically — `@Startup` would be redundant.

### Annotation-Based Metrics

Use `@Timed` and `@Counted` on CDI bean methods for simple cases:

```java
@Path("/greeting")
public class GreetingResource {
    @GET
    @Timed(value = "greeting.time", description = "Time to generate a greeting")
    @Counted(value = "greeting.count", description = "Number of greetings generated")
    public String greeting(@QueryParam("name") String name) {
        return "Hello, " + name + "!";
    }
}
```

Use `@MeterTag` (`io.micrometer.core.aop.MeterTag`) on method parameters to add dynamic tags derived from arguments:

```java
@Counted(value = "metric.all", extraTags = { "extra", "tag" })
public Object countAllInvocations(@MeterTag boolean fail) {
    // tag "fail" is auto-derived from the parameter name and value
}
```

Annotations only work on CDI bean methods — not on `private` methods, not on instances created with `new`.

### MeterFilter — Global Metric Customization

Produce `MeterFilter` CDI beans to customize metrics globally — add common tags, configure histograms and percentiles, rename metrics, or deny/accept specific meters:

### Dynamic Per-Request Tags — Use HttpServerMetricsTagsContributor

Implement `HttpServerMetricsTagsContributor` only when the tag value depends on the HTTP request (a header, a path parameter, a query parameter):

```java
@Singleton
public class ApiVersionTagContributor implements HttpServerMetricsTagsContributor {
    @Override
    public Tags contribute(Context context) {
        String version = context.request().getHeader("X-Api-Version");
        return Tags.of("api.version", version != null ? version : "unknown");
    }
}
```

Do NOT use `HttpServerMetricsTagsContributor` for static tags. That interface is for dynamic, per-request tags only.
A matching `HttpClientMetricsTagsContributor` exists for outbound HTTP client metrics.

### Customizing Meter Registries

Produce a `MeterRegistryCustomizer` CDI bean to apply programmatic customizations to registries:

```java
@Produces
@Singleton
public MeterRegistryCustomizer customizeAllRegistries() {
    return registry -> registry.config()
            .meterFilter(MeterFilter.ignoreTags("too.verbose"));
}
```

### Testing Metrics

Tests must verify that specific metric names appear in the Prometheus endpoint after exercising the application:

```java
@QuarkusTest
class OrderResourceTest {
    @Test
    void metricsEndpointContainsCustomMetrics() {
        // Exercise the endpoints to generate metrics
        given().post("/orders").then().statusCode(200);
        given().header("X-Api-Version", "v2").get("/greeting?name=Test").then().statusCode(200);
        given().post("/tasks").then().statusCode(200);
        // Assert full metric line: name + tags + value in a single containsString
        given()
            .when().get("/q/metrics")
            .then()
            .statusCode(200)
            // MeterProvider counter
            .body(containsString(
                    "orders_placed_total{application=\"my-app\",environment=\"dev\",type=\"standard\"} 1.0"))
            // @Timed annotation
            .body(containsString(
                    "greeting_time_seconds_count{application=\"my-app\",class=\"com.demo.GreetingResource\",environment=\"dev\",exception=\"none\",method=\"greeting\"} 1.0"))
            // @Counted annotation
            .body(containsString(
                    "greeting_count_total{application=\"my-app\",class=\"com.demo.GreetingResource\",environment=\"dev\",exception=\"none\",method=\"greeting\",result=\"success\"} 1.0"))
            // HttpServerMetricsTagsContributor adds api_version from the request header
            .body(containsString(
                    "http_server_requests_seconds_count{api_version=\"v2\",application=\"my-app\",environment=\"dev\",method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/greeting\"} 1.0"))
            // Gauge tracking queue size
            .body(containsString(
                    "queue_depth{application=\"my-app\",environment=\"dev\"} 1.0"))
            // @Counted with @MeterTag
            .body(containsString(
                    "metric_all_total{application=\"my-app\",class=\"com.demo.AnnotatedService\",environment=\"dev\",exception=\"none\",extra=\"tag\",fail=\"false\",method=\"countAllInvocations\",result=\"success\"} 1.0"));
    }
}
```

The tests use `rest-assured`.
Checking only that `/q/metrics` returns 200 is not a meaningful test.
MUST only Assert the full metric line: name + tags + value in a SINGLE `containsString`. Don't check only isolated name and tags because it only proves the meter was registered, not that it's recording correctly.
Do not extract the response body as a `String` — use rest-assured's `.body(containsString(...))` directly. Hand-rolled helpers tend to silently drop the value assertion.
With @QuarkusTest, the MeterRegistry is shared across test methods and cannot be reset between them — counters, timers, and summaries accumulate. If a class has multiple test methods, each must assert on distinct metrics or distinct tag combinations. Otherwise, the second test sees values left by the first.

### Common Pitfalls

- **Avoid adding default configurations.** No need to add configurations to `application.properties` if the required values are already default.
- **Do not mix OTel and Micrometer config.** `quarkus.otel.*` properties do not configure Micrometer. Use `quarkus.micrometer.*`.
- **Publish percentiles on timers and distribution summaries.** Use `.publishPercentiles(0.5, 0.95, 0.99)` — without this, you only get count/sum/max, which are insufficient for latency analysis.
- **Prometheus naming.** Dots become underscores. A timer named `my.timer` appears as `my_timer_seconds_*` in `/q/metrics`. A counter named `my.counter` appears as `my_counter_total`.
- **In `Timer` or `DistributionSummary`** do not publish percentiles and service level objectives for the same metric.
- **Don't create high cardinality tags**. Tags like `item.id`, with unique values per item creates unbounded time series — an antipattern.
