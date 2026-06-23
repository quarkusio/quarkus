
### When to Use

Use `quarkus-kafka-client` for direct access to the Apache Kafka Producer/Consumer/AdminClient APIs. For most messaging use cases, prefer `quarkus-messaging-kafka` (Reactive Messaging) instead — it provides annotation-driven consuming/producing with auto-serialization.

### Configuration

Inject the auto-configured Kafka config map:

```java
@Inject
@Identifier("default-kafka-broker")
Map<String, Object> kafkaConfig;
```

This map includes `bootstrap.servers` and other properties from Dev Services or `application.properties`. Add serializer/deserializer/group config when creating producers or consumers.

### Creating a Producer

Put the `@Produces` method in a **separate** CDI bean to avoid proxy issues:

```java
@ApplicationScoped
public class KafkaProducerFactory {
    @Produces @ApplicationScoped
    KafkaProducer<String, String> createProducer(@Identifier("default-kafka-broker") Map<String, Object> config) {
        var props = new HashMap<>(config);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    void closeProducer(@Disposes KafkaProducer<String, String> producer) {
        producer.close();
    }
}
```

Then inject it where needed: `@Inject KafkaProducer<String, String> producer;`

- Always add a `@Disposes` method to close the producer on shutdown.
- For JSON values, use `ObjectMapperSerializer.class.getName()` — it has a no-arg constructor.

### Creating a Consumer

Consumers block during `poll()`, so run them in a background thread:

```java
@Inject @Identifier("default-kafka-broker") Map<String, Object> config;
volatile boolean running;
KafkaConsumer<String, String> consumer;

void onStart(@Observes StartupEvent ev) {
    var props = new HashMap<>(config);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-group");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumer = new KafkaConsumer<>(props);
    consumer.subscribe(List.of("my-topic"));
    running = true;
    new Thread(this::pollLoop).start();
}

void onStop(@Observes ShutdownEvent ev) {
    running = false;
    consumer.wakeup();
}

void pollLoop() {
    try {
        while (running) {
            var records = consumer.poll(Duration.ofMillis(500));
            records.forEach(r -> process(r));
        }
    } catch (WakeupException e) {
        // expected on shutdown
    } finally {
        consumer.close();
    }
}
```

- Set `auto.offset.reset=earliest` to consume from the beginning (default is `latest`).
- Use `consumer.wakeup()` for graceful shutdown — it causes `poll()` to throw `WakeupException`.
- Store consumed data in thread-safe collections (`CopyOnWriteArrayList`, `ConcurrentHashMap`).

### AdminClient

```java
try (AdminClient admin = AdminClient.create(kafkaConfig)) {
    admin.createTopics(List.of(new NewTopic("my-topic", 3, (short) 1))).all().get();
    Set<String> topics = admin.listTopics().names().get();
}
```

### Serialization Utilities

Quarkus provides Jackson-based serializers in `io.quarkus.kafka.client.serialization`:
- `ObjectMapperSerializer<T>` — has a no-arg constructor, usable directly.
- `ObjectMapperDeserializer<T>` — requires a subclass passing the target class:
  ```java
  public class EventDeserializer extends ObjectMapperDeserializer<Event> {
      public EventDeserializer() { super(Event.class); }
  }
  ```

### Dev Services

Kafka starts automatically in dev and test mode — no configuration needed. Uses `apache/kafka-native` container. Topics are auto-created on first use.

### Testing

- Kafka consumers take several seconds to join the consumer group. Use Awaitility (add `org.awaitility:awaitility` with test scope).
- Dev Services provides a real broker in tests — no mocking needed.

### Common Pitfalls

- `@Identifier("default-kafka-broker")` is the qualifier for the auto-configured config map — this is not obvious but essential.
- Never call `KafkaConsumer.poll()` on the main thread or CDI request thread — it blocks.
- Always close producers and consumers on shutdown to avoid resource leaks.
