
### Producing Messages

Inject an `Emitter` with `@Channel` to send messages to an AMQP address:

```java
@Inject @Channel("orders-out")
Emitter<JsonObject> emitter;

@POST @Path("/orders")
public CompletionStage<Response> create(Order order) {
    return emitter.send(JsonObject.mapFrom(order))
        .thenApply(x -> Response.accepted().build());
}
```

### Consuming Messages

Use `@Incoming` on a CDI bean method:

```java
@ApplicationScoped
public class OrderProcessor {

    @Incoming("orders-in")
    public void process(JsonObject json) {
        Order order = json.mapTo(Order.class);
        // process order
    }
}
```

### Serialization

The AMQP connector natively supports: primitives, `String`, `Instant`, `UUID`, `JsonObject`, `JsonArray`, and `Buffer`. For custom POJOs, serialize to `JsonObject`:

```java
// Sending: POJO → JsonObject
JsonObject json = JsonObject.mapFrom(myPojo); // io.vertx.core.json.JsonObject
emitter.send(json);

// Receiving: JsonObject → POJO
Order order = json.mapTo(Order.class);
```

### Configuration

```properties
# Outgoing channel (producer)
mp.messaging.outgoing.orders-out.connector=smallrye-amqp
mp.messaging.outgoing.orders-out.address=orders

# Incoming channel (consumer)
mp.messaging.incoming.orders-in.connector=smallrye-amqp
mp.messaging.incoming.orders-in.address=orders
```

The connector name is `smallrye-amqp`. If `address` is omitted, the channel name is used as the AMQP address.

### Dev Services

An ActiveMQ Artemis broker starts automatically in dev/test mode — no broker configuration needed. The `amqp-host` and `amqp-port` are auto-configured.

### AMQP Metadata

Read incoming message metadata:

```java
@Incoming("orders-in")
public void process(Message<JsonObject> message) {
    Optional<IncomingAmqpMetadata> metadata = message.getMetadata(IncomingAmqpMetadata.class);
    metadata.ifPresent(m -> {
        String subject = m.getSubject();
        String contentType = m.getContentType();
    });
    message.ack();
}
```

Set outgoing message properties:

```java
OutgoingAmqpMetadata metadata = OutgoingAmqpMetadata.builder()
    .withSubject("order-created")
    .withContentType("application/json")
    .build();
emitter.send(Message.of(json).addMetadata(metadata));
```

### Common Pitfalls

- **Sending POJOs directly fails** — AMQP doesn't auto-serialize POJOs. Use `JsonObject.mapFrom(pojo)`. The error is a cryptic `NullPointerException` in context propagation, not a clear serialization error.
- **Connector name is `smallrye-amqp`** — not `amqp` or `messaging-amqp`.
- **Extension artifact is `quarkus-messaging-amqp`** — not the full SmallRye name.
- **Channel name vs address**: The channel name (in `@Channel`/`@Incoming`) is the config key. The `address` property is the AMQP queue/topic name on the broker. They can differ.
- **`Emitter.send()` returns `CompletionStage<Void>`** — use it to confirm the message was sent before responding to the HTTP request.
