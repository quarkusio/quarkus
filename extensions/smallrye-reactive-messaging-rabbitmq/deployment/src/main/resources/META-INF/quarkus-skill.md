
### Setting up channels

The `quarkus-messaging-rabbitmq` extension maps Reactive Messaging channels to RabbitMQ. `incoming` channels are bound to queues, `outgoing` channels are bound to exchanges:

```properties
# Inbound channel -> RabbitMQ queue
mp.messaging.incoming.prices.connector=smallrye-rabbitmq
mp.messaging.incoming.prices.queue.name=prices-queue
mp.messaging.incoming.prices.exchange.name=prices-exchange

# Outbound channel -> RabbitMQ exchange
mp.messaging.outgoing.orders.connector=smallrye-rabbitmq
mp.messaging.outgoing.orders.exchange.name=orders-exchange
```

If `queue.name` or `exchange.name` is not set, the channel name is used. To bind to a pre-existing topology, set `queue.declare=false` and `exchange.declare=false`.

Broker access is configured globally (or per-channel via the same attribute without the `rabbitmq-` prefix):

```properties
rabbitmq-host=localhost
rabbitmq-port=5672
rabbitmq-username=guest
rabbitmq-password=guest
rabbitmq-virtual-host=/
```

### Producing messages

Use `@Outgoing` to publish from a method, or inject an `Emitter` for imperative sends:

```java
@ApplicationScoped
public class PricePublisher {

    @Channel("orders")
    Emitter<Order> emitter;

    public void publish(Order order) {
        emitter.send(order);
    }

    @Outgoing("prices")
    public Multi<Price> stream() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                .map(t -> new Price(t.intValue()));
    }
}
```

Payloads are serialized as follows:

| Payload type                                  | RabbitMQ body / `content_type`                  |
|-----------------------------------------------|-------------------------------------------------|
| primitives, `String`, `UUID`                  | text body, `text/plain`                         |
| Vert.x `JsonObject` / `JsonArray`             | JSON body, `application/json`                   |
| `byte[]` or Vert.x `Buffer`                   | binary body, `application/octet-stream`         |
| any other class                               | JSON body (Jackson), `application/json`         |

If a payload cannot be serialized to JSON, the message is nacked.

### Outbound metadata

Attach `OutgoingRabbitMQMetadata` to set routing keys, headers, timestamps, expiration, and priority:

```java
return Message.of(payload, Metadata.of(
        new OutgoingRabbitMQMetadata.Builder()
                .withRoutingKey("urgent")
                .withHeader("tenant", "acme")
                .withTimestamp(ZonedDateTime.now())
                .withExpiration("60000")
                .build()));
```

The default routing key is configured via the `default-routing-key` channel attribute.

### Consuming messages

Consume the payload directly, or use `Message<T>` for explicit acknowledgement and metadata access:

```java
@Incoming("prices")
public void consume(double price) {
    // process
}

@Incoming("prices")
public CompletionStage<Void> consume(Message<Double> msg) {
    Optional<IncomingRabbitMQMetadata> meta = msg.getMetadata(IncomingRabbitMQMetadata.class);
    meta.ifPresent(m -> {
        Optional<String> routingKey = m.getRoutingKey();
        Optional<String> contentType = m.getContentType();
        Optional<String> headerValue = m.getHeader("tenant", String.class);
        Map<String, Object> allHeaders = m.getHeaders();
    });
    return msg.ack();
}
```

Incoming payloads are decoded based on the RabbitMQ `content_type` and `content_encoding`:

- any `content_encoding` set -> `byte[]`
- `text/plain` -> `String`
- `application/json` -> Vert.x `JsonObject` / `JsonArray` / `String` (use `JsonObject.mapTo(MyClass.class)` to materialize)
- anything else -> `byte[]`

Override the detected type with `content-type-override` on the incoming channel.

### Blocking processing

Reactive Messaging invokes consumer methods on an I/O thread. For blocking work (JDBC, JPA), annotate with `@Blocking`:

```java
@Incoming("prices")
@Blocking
@Transactional
public void store(int priceInUsd) {
    Price entity = new Price();
    entity.value = priceInUsd;
    entity.persist();
}
```

Two `@Blocking` annotations are interchangeable for the basic case: `io.smallrye.reactive.messaging.annotations.Blocking` (allows worker pool + ordering tuning) and `io.smallrye.common.annotation.Blocking` (default pool, ordered). For virtual threads, use `@RunOnVirtualThread` instead.

### Acknowledgement and failure strategy

Inbound messages are acked when the corresponding `Message` is acked. If `auto-acknowledgement=true`, delivery itself counts as ack. When a message is nacked, the channel's `failure-strategy` decides what happens:

- `reject` (default) — the RabbitMQ message is rejected; processing continues.
- `accept` — the message is acked; processing continues.
- `fail` — the application is failed; no more messages are processed.

For poison-message handling, enable `auto-bind-dlq=true` and optionally override `dead-letter-queue-name`, `dead-letter-exchange`, `dead-letter-routing-key`, and `dlx.declare`.

### Customizing the underlying client

Produce a named `RabbitMQOptions` bean and reference it via `client-options-name`:

```java
@Produces
@Identifier("my-named-options")
public RabbitMQOptions options() {
    return new RabbitMQOptions()
            .setHost("rabbit.internal")
            .setPort(5672)
            .setVirtualHost("/tenant-a")
            .setRequestedHeartbeat(60)
            .setAutomaticRecoveryEnabled(true);
}
```

```properties
mp.messaging.incoming.prices.client-options-name=my-named-options
```

For TLS, configure a named entry in the Quarkus TLS registry and reference it with `tls-configuration-name=your-tls-config` on the channel — do not configure `ssl`, `trust-store-path`, etc. directly when a TLS registry config is available.

### Dev Services

In dev and test modes, Quarkus auto-starts a RabbitMQ container with the management plugin enabled. Dev Services is skipped if `rabbitmq-host`/`rabbitmq-port` is set or `quarkus.rabbitmq.devservices.enabled=false`.

Predefine the broker topology so consumers/producers see exchanges, queues, and bindings on startup:

```properties
quarkus.rabbitmq.devservices.exchanges.orders.type=topic
quarkus.rabbitmq.devservices.queues.orders-queue.durable=true
quarkus.rabbitmq.devservices.bindings.orders-binding.source=orders
quarkus.rabbitmq.devservices.bindings.orders-binding.destination=orders-queue
quarkus.rabbitmq.devservices.bindings.orders-binding.routing-key=new
```

Containers are shared across applications in dev mode (use `quarkus.rabbitmq.devservices.service-name` to scope the share). Sharing is disabled in test mode by default.

### Testing

Dev Services starts a broker automatically, so most `@QuarkusTest` tests need no configuration. Use `InMemoryConnector` to replace the broker entirely when the test should run without a container:

```java
public class InMemoryProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> env = new HashMap<>();
        env.putAll(InMemoryConnector.switchIncomingChannelsToInMemory("prices"));
        env.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory("orders"));
        return env;
    }
}

@QuarkusTest
@TestProfile(InMemoryProfile.class)
class OrderFlowTest {

    @Inject @Any
    InMemoryConnector connector;

    @Test
    void publishesOrder() {
        InMemorySource<Integer> prices = connector.source("prices");
        InMemorySink<Order> orders = connector.sink("orders");

        prices.send(42);

        await().untilAsserted(() -> assertThat(orders.received())
                .extracting(Message::getPayload)
                .containsExactly(new Order(42)));
    }
}
```

The `quarkus-messaging-test` test dependency provides `InMemoryConnector`.

### Health reporting

When `quarkus-smallrye-health` is present, each channel contributes to the liveness and readiness probes. Disable per channel with `health-enabled=false`. The `fail` failure strategy reports failures to the health check; `accept` and `reject` do not.

### Common Pitfalls

- **`outgoing` -> exchange, `incoming` -> queue.** Confusing the two is the usual source of "channel not found" or empty consumers. The exchange/queue name defaults to the channel name if not set.
- **`smallrye-rabbitmq` is AMQP 0-9-1.** RabbitMQ 4.x also speaks AMQP 1.0, but that protocol requires the separate `quarkus-messaging-amqp` connector — they are not interchangeable.
- **Use `@RegisterForReflection` on payload classes.** Without it, JSON (de)serialization fails in native mode after dead-code elimination removes the fields.
- **Pre-existing topology requires `declare=false`.** If the queue or exchange already exists with different attributes (durability, type), declaration fails. Set `queue.declare=false` / `exchange.declare=false` to bind without re-declaring.
- **TTL and DLQ properties only apply when the queue is declared by the connector.** They have no effect when binding to a queue that already exists.
- **Do not configure `ssl`/`trust-store-path` alongside `tls-configuration-name`.** The TLS registry config takes precedence and the channel-level TLS attributes are ignored.
- **`@Blocking` is required for blocking work.** Without it, JDBC or `Thread.sleep` calls inside `@Incoming` methods will block the I/O thread and stall the broker connection.
