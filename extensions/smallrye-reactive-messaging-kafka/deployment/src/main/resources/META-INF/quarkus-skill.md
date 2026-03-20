### Kafka Connector Configuration

- `mp.messaging.incoming.my-channel.connector=smallrye-kafka`
- `mp.messaging.incoming.my-channel.topic=my-topic`
- `mp.messaging.incoming.my-channel.group.id=my-group`
- `mp.messaging.outgoing.my-channel.connector=smallrye-kafka`

### Serialization

- `mp.messaging.incoming.my-channel.value.deserializer=org.apache.kafka.common.serialization.StringDeserializer`
- For JSON: use `io.quarkus.kafka.client.serialization.JsonObjectDeserializer` or generate with `@RegisterForReflection`.

### Kafka-Specific Features

- Access Kafka metadata via `IncomingKafkaRecordMetadata` from `Message.getMetadata()`.
- Set keys with `OutgoingKafkaRecordMetadata`.
- Consumer group auto-assigned from `group.id` config.

### Dev Services

- Redpanda Kafka broker starts automatically — no config needed.
- Pre-create topics: `quarkus.kafka.devservices.topic-partitions.my-topic=3`.

### Testing

- Use in-memory connector: `mp.messaging.incoming.my-channel.connector=smallrye-in-memory` in test config.
- Or test with Dev Services Kafka for integration tests.

### Common Pitfalls

- Do NOT set `kafka.bootstrap.servers` without `%prod.` prefix — disables Dev Services.
- Always set `group.id` for consumer channels.
