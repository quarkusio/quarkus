### Core Concepts

- `@Incoming("channel")` — consume messages from a channel.
- `@Outgoing("channel")` — produce messages to a channel.
- `@Channel("channel") Emitter<T>` — imperatively send messages.
- Channels connect to external systems via connectors (Kafka, AMQP, RabbitMQ, etc.).

### Processing Patterns

- Method with `@Incoming` only — consumer/sink.
- Method with `@Outgoing` only — producer/source.
- Method with both `@Incoming` and `@Outgoing` — processor/transformer.

### Message vs Payload

- Accept `Message<T>` for access to metadata and manual acknowledgment.
- Accept plain `T` for automatic acknowledgment (simpler).

### In-Memory Connector for Testing

- Set `mp.messaging.incoming.channel.connector=smallrye-in-memory` in test config.
- Inject `InMemoryConnector` to send/receive messages in tests.

### Common Pitfalls

- Method return type determines behavior: `void` = sink, `T` = processor.
- Do NOT block on reactive threads — use `@Blocking` for blocking operations in message handlers.
