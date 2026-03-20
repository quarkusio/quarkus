### AMQP Connector Configuration

- `mp.messaging.incoming.my-channel.connector=smallrye-amqp`
- `mp.messaging.incoming.my-channel.address=my-queue`
- `mp.messaging.outgoing.my-channel.connector=smallrye-amqp`

### Dev Services

- An ActiveMQ Artemis broker starts automatically in dev/test.

### Testing

- Use in-memory connector for unit tests.
- Use Dev Services for integration tests.

### Common Pitfalls

- Do NOT set `amqp-host`/`amqp-port` without profile prefix — disables Dev Services.
