
### Minimal Working Pattern

The simplest producer→consumer setup uses the **default exchange** with a queue name matching the channel:

```properties
# Outgoing — sends to default exchange ("") with routing key = queue name
mp.messaging.outgoing.tasks.connector=smallrye-rabbitmq
mp.messaging.outgoing.tasks.default-routing-key=tasks
mp.messaging.outgoing.tasks.exchange.name=

# Incoming — consumes from queue "tasks"
mp.messaging.incoming.tasks-in.connector=smallrye-rabbitmq
mp.messaging.incoming.tasks-in.queue.name=tasks
```

The connector auto-declares the queue. No exchange or binding configuration needed for this basic pattern.

### Producing Messages

```java
@Inject @Channel("tasks")
Emitter<JsonObject> emitter;

@POST @Path("/tasks")
public CompletionStage<Response> create(Task task) {
    return emitter.send(JsonObject.mapFrom(task))
        .thenApply(x -> Response.accepted().build());
}
```

### Consuming Messages

```java
@ApplicationScoped
public class TaskConsumer {

    @Incoming("tasks-in")
    public void process(JsonObject json) {
        Task task = json.mapTo(Task.class);
        // process task
    }
}
```

### Serialization

RabbitMQ connector requires `JsonObject` for custom POJOs — same as AMQP:

```java
// Send: JsonObject.mapFrom(pojo)    — io.vertx.core.json.JsonObject
// Receive: json.mapTo(Task.class)   — deserialize back to POJO
```

Sending POJOs directly causes a `ClassCastException` (BufferImpl cannot be cast).

### Named Exchange Pattern

For topic/fanout routing, configure an exchange explicitly:

```properties
# Outgoing — publish to a named exchange
mp.messaging.outgoing.events.connector=smallrye-rabbitmq
mp.messaging.outgoing.events.exchange.name=my-events
mp.messaging.outgoing.events.exchange.type=topic
mp.messaging.outgoing.events.default-routing-key=order.created

# Incoming — consume from a queue bound to the exchange
mp.messaging.incoming.events-in.connector=smallrye-rabbitmq
mp.messaging.incoming.events-in.queue.name=order-events
mp.messaging.incoming.events-in.queue.routing-keys=order.*
mp.messaging.incoming.events-in.exchange.name=my-events
```

Exchange types: `direct` (default), `fanout`, `topic`, `headers`.

### Dev Services

A RabbitMQ container (with management UI) starts automatically in dev/test mode. No broker configuration needed. The management UI is accessible for debugging queue state.

### RabbitMQ Metadata

Read incoming message properties:

```java
@Incoming("tasks-in")
public void process(Message<JsonObject> message) {
    message.getMetadata(IncomingRabbitMQMetadata.class).ifPresent(m -> {
        String routingKey = m.getRoutingKey();
        String exchange = m.getExchange();
    });
    message.ack();
}
```

### Common Pitfalls

- **Connector name is `smallrye-rabbitmq`** — not `rabbitmq` or `messaging-rabbitmq`.
- **Custom POJOs need JsonObject** — use `JsonObject.mapFrom(pojo)` to send, `json.mapTo(Class)` to receive. Direct POJO sending causes `ClassCastException`.
- **Exchange-queue binding**: With named exchanges, the incoming channel must specify both `exchange.name` and `queue.name` — the connector creates the binding automatically when both are set.
- **Default exchange**: Using `exchange.name=` (empty string) routes directly to a queue by name via the `default-routing-key`.
- **Routing key mismatch**: For topic exchanges, the outgoing `default-routing-key` must match the incoming `queue.routing-keys` pattern. For direct exchanges, they must match exactly.
- **Queue and exchange auto-declaration**: The connector auto-declares queues and exchanges by default (`declare=true`). Set `declare=false` if using pre-existing infrastructure. Don't mix with Dev Services topology or you may get durable-flag conflicts.
