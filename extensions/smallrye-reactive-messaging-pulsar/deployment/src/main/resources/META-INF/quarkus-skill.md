### Pulsar Connector

- `mp.messaging.incoming.my-channel.connector=smallrye-pulsar`
- `mp.messaging.incoming.my-channel.topic=my-topic`
- `mp.messaging.outgoing.my-channel.connector=smallrye-pulsar`

### Schema

- Configure schema: `mp.messaging.incoming.my-channel.schema=STRING`.

### Dev Services

- A Pulsar container starts automatically in dev/test.

### Common Pitfalls

- Do NOT set `pulsar.client.serviceUrl` without a profile prefix — disables Dev Services.
