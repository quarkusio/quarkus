### RabbitMQ Connector

- `mp.messaging.incoming.my-channel.connector=smallrye-rabbitmq`
- `mp.messaging.incoming.my-channel.queue.name=my-queue`
- `mp.messaging.outgoing.my-channel.connector=smallrye-rabbitmq`
- `mp.messaging.outgoing.my-channel.exchange.name=my-exchange`

### Dev Services

- A RabbitMQ container starts automatically in dev/test.

### Testing

- Use in-memory connector for unit tests.
- Use Dev Services for integration tests.

### Common Pitfalls

- Do NOT set `rabbitmq-host` without a profile prefix — disables Dev Services.
