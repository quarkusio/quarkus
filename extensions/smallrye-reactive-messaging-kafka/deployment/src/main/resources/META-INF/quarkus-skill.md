
### Consuming Messages

```java
@Incoming("prices")
public void consume(double price) {
    // called for each Kafka record on the "prices" channel
}
```

- `@Incoming("channel-name")` consumes from a Kafka topic.
- Method can accept: the payload directly, `Message<T>`, `ConsumerRecord<K,V>`, or `Record<K,V>`.
- Use `@Blocking` if the processing method performs blocking I/O.

### Producing Messages

```java
@Outgoing("generated-prices")
public Multi<Double> generate() {
    return Multi.createFrom().ticks().every(Duration.ofSeconds(1)).map(x -> random.nextDouble());
}
```

### Processing (Incoming → Outgoing)

```java
@Incoming("requests")
@Outgoing("responses")
public ProcessedOrder process(Order order) {
    return new ProcessedOrder(order.id(), order.item(), order.quantity(), order.quantity() * 10.0);
}
```

### Sending from Imperative Code

```java
@Channel("orders")
Emitter<Order> emitter;

public void send(Order order) {
    emitter.send(order);
}
```

- Use `@Channel` (not `@Outgoing`) to inject an `Emitter<T>`.
- For Kafka keys: `emitter.send(Record.of(key, value))`.
- Backpressure: use `@OnOverflow(strategy = BUFFER, bufferSize = 512)` if the emitter queues up.

### Channel vs Topic Names

Channel names in `@Incoming`, `@Outgoing`, and `@Channel` must be **unique**. If you need to produce to and consume from the SAME Kafka topic, use different channel names:

```properties
mp.messaging.outgoing.orders-out.topic=orders
mp.messaging.incoming.orders-in.topic=orders
```

Then use `@Outgoing("orders-out")` / `@Channel("orders-out")` for producing and `@Incoming("orders-in")` for consuming.

### Serialization

**Do NOT set `value.serializer` or `value.deserializer` explicitly** — Quarkus auto-detects them at build time from your method signatures and `Emitter<T>` types. It generates Jackson-based serializers/deserializers automatically.

Only set them manually if you need a specific serializer (e.g., Avro, Protobuf).

### Error Handling

- `mp.messaging.incoming.my-channel.failure-strategy=dead-letter-queue` — failed messages go to `dead-letter-topic-{topic}`.
- `mp.messaging.incoming.my-channel.failure-strategy=ignore` — skip failed messages.
- Combine with SmallRye Fault Tolerance: `@Retry(delay = 10, maxRetries = 5)` on `@Incoming` methods.

### Accessing Kafka Metadata

Use `ConsumerRecord<K,V>` as the parameter type to access key, partition, offset, headers:

```java
@Incoming("orders")
public void consume(ConsumerRecord<String, Order> record) {
    String key = record.key();
    int partition = record.partition();
    long offset = record.offset();
    String topic = record.topic();
    Headers headers = record.headers();
}
```

For manual acknowledgment with metadata, use `Message<T>` and return `CompletionStage<Void>`:

```java
@Incoming("orders")
public CompletionStage<Void> consume(Message<Order> message) {
    Order payload = message.getPayload();
    // process, then acknowledge
    return message.ack();
}
```

### Dev Services

Kafka Dev Service starts automatically — no configuration needed. Uses `apache/kafka-native` container. Topics are auto-created on first use.

### Testing

- Kafka consumers take several seconds to join the consumer group after app startup. Use `Awaitility` or sufficient timeouts in tests:
  ```java
  await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
      // assert processed results are available
  });
  ```
- Dev Services provides a real Kafka broker in tests — no mocking needed.
- For unit testing without Kafka, add `quarkus-test-reactive-messaging` and use `InMemoryConnector`.

### Common Pitfalls

- Channel names must be unique — you cannot use the same name for both `@Incoming` and `@Outgoing`/`@Channel`.
- Do NOT set serializer/deserializer config unless you have a specific reason — auto-detection handles it.
- `Emitter.send()` is asynchronous — the message may not be on Kafka when the method returns.
- `@Incoming` methods returning `void` use auto-acknowledgment. Use `Message<T>` parameter and call `message.ack()` for manual control.
- Consumer group ID defaults to `quarkus.application.name` + channel name. Set `mp.messaging.incoming.my-channel.group.id` to override.
