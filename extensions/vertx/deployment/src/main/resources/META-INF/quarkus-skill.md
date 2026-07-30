
### Event Bus Consumer

Use `@ConsumeEvent` on a CDI bean method to register an event bus consumer:

```java
@ApplicationScoped
public class NotificationConsumer {

    @ConsumeEvent("notifications")
    public void handle(String message) {
        // fire-and-forget: no reply
    }

    @ConsumeEvent("echo")
    public String echo(String message) {
        return message.toUpperCase(); // reply is sent automatically
    }

    @ConsumeEvent(value = "heavy-work", blocking = true)
    public String processBlocking(String data) {
        // runs on a worker thread, not the event loop
        return expensiveComputation(data);
    }
}
```

**Parameter options** for `@ConsumeEvent` methods:
- `T body` — receives the message body directly
- `Message<T> msg` — full Vert.x message (must return `void`, call `msg.reply()` manually)
- `MultiMap headers, T body` — headers as first param, body as second

**Return type rules**: If the method accepts `Message<T>`, it must return `void`. For any other parameter type, the return value is sent as the reply. Supports `String`, POJOs, `Uni<T>`, and `CompletionStage<T>`.

### Sending Messages

Inject `EventBus` (Mutiny variant) to send messages programmatically:

```java
@Inject
io.vertx.mutiny.core.eventbus.EventBus bus;

// Fire-and-forget to one consumer
bus.send("notifications", "Hello");

// Broadcast to all consumers on the address
bus.publish("notifications", "Hello everyone");

// Request-reply (returns Uni)
Uni<String> reply = bus.request("echo", "ping")
    .onItem().transform(msg -> msg.body().toString());
```

`send()` delivers to **one** consumer (point-to-point). `publish()` delivers to **all** consumers on the address.

### Injecting Vert.x

```java
@Inject io.vertx.mutiny.core.Vertx vertx;
```

Always use the `io.vertx.mutiny.*` package — the Mutiny variants return `Uni`/`Multi` and integrate with Quarkus reactive.

### Address Configuration

The address defaults to the bean's fully qualified class name. Use `value` to set a custom address. Config property expressions are supported:

```java
@ConsumeEvent("${my.address:default-address}")
```

### Local vs Clustered

By default, consumers are `local = true` (same JVM only). Set `local = false` for clustered event bus delivery.

### POJOs on the Event Bus

POJOs are automatically serialized/deserialized using a local codec within the same JVM — no special configuration needed. The built-in `LocalEventBusCodec` handles this.

### Common Pitfalls

- **`send()` vs `publish()`**: `send()` delivers to one consumer, `publish()` delivers to all. A common mistake is using `send()` when multiple consumers should receive the message.
- **`request()` needs a reply**: If you use `bus.request()`, the consumer must return a value. A void consumer causes a timeout.
- **Blocking = false by default**: `@ConsumeEvent` runs on the event loop. Long-running operations will block the event loop. Use `blocking = true` or `@io.smallrye.common.annotation.Blocking`.
- **Mutiny imports**: Use `io.vertx.mutiny.core.Vertx` and `io.vertx.mutiny.core.eventbus.EventBus`, not the bare Vert.x types.
- **CDI request context is active**: The CDI request context is always active during `@ConsumeEvent` invocations.
- **Exception handling**: If a consumer throws, the sender receives a `ReplyException` with code `0x1FF9`. Failed `Uni`/`CompletionStage` returns use code `0x1FFF`.
