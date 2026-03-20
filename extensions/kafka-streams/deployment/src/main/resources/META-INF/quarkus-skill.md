### Topology Definition

- Define a `@Produces Topology` method in a CDI bean.
- Or implement `TopologyProducer` interface.
- Use `StreamsBuilder` to build the topology.

### Interactive Queries

- Inject `KafkaStreams` for state store queries.
- Use `streams.store(StoreQueryParameters.fromNameAndType("store", QueryableStoreTypes.keyValueStore()))`.

### Dev Services

- Kafka Dev Services (Redpanda) starts automatically.

### Testing

- Use `TopologyTestDriver` for unit testing topologies without a Kafka broker.
- Use `@QuarkusTest` with Dev Services for integration tests.

### Common Pitfalls

- State stores are local to each instance — use global stores or interactive queries for cross-instance access.
- Kafka Streams requires explicit serialization config.
