### Producer and Consumer

- Inject `KafkaProducer<K, V>` or `KafkaConsumer<K, V>` for direct Kafka access.
- For most use cases, prefer Reactive Messaging (`quarkus-smallrye-reactive-messaging-kafka`) which provides `@Incoming`, `@Outgoing`, and `Emitter` APIs.
- Use the raw Kafka client only when you need fine-grained control over partitions, offsets, or admin operations.

### Serialization

- Configure serializers/deserializers in `application.properties`.
- Use `quarkus.kafka.devservices.topic-partitions.my-topic=3` to pre-create topics.

### Dev Services

- A Kafka broker (Redpanda) starts automatically in dev/test — no config needed.

### Testing

- Use `@QuarkusTest` with Dev Services Kafka.
- For Reactive Messaging, use the in-memory connector for fast unit tests.

### Common Pitfalls

- Prefer Reactive Messaging over raw Kafka client for typical produce/consume patterns.
- Do NOT set `kafka.bootstrap.servers` without a profile prefix — this disables Dev Services.
