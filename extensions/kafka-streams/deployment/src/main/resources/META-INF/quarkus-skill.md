
### Creating a Topology

Declare a CDI producer that returns a Kafka Streams `Topology` — Quarkus manages the lifecycle:

```java
@ApplicationScoped
public class WordCountTopology {

    static final String WORD_COUNT_STORE = "word-counts-store";

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        builder.<String, String>stream("input-words")
            .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
            .groupBy((key, word) -> word, Grouped.with(Serdes.String(), Serdes.String()))
            .count(Materialized.as(
                Stores.persistentKeyValueStore(WORD_COUNT_STORE)))
            .toStream()
            .to("word-counts", Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }
}
```

- The `@Produces Topology` method is the entry point — Quarkus auto-starts the Kafka Streams engine.
- Specify serdes explicitly in the topology code (e.g., `Grouped.with(...)`, `Produced.with(...)`) rather than relying on config defaults.
- Use `Stores.persistentKeyValueStore(name)` with `Materialized.as(...)` for state stores.

### Configuration

```properties
quarkus.kafka-streams.application-id=my-streams-app
quarkus.kafka-streams.topics=input-words,word-counts
```

- `application-id` is required — identifies your streams app for consumer groups and state stores.
- `topics` lists the input topics the app depends on — Quarkus waits for them to exist before starting.
- Kafka Dev Services auto-starts a broker. Topics are auto-created on first use.

### Interactive Queries

Query local state stores via the injected `KafkaStreams` instance:

```java
@Inject KafkaStreams streams;

@GET @Path("/word-count/{word}")
public Response getWordCount(@PathParam("word") String word) {
    ReadOnlyKeyValueStore<String, Long> store = streams.store(
        StoreQueryParameters.fromNameAndType(WORD_COUNT_STORE, QueryableStoreTypes.keyValueStore()));
    Long count = store.get(word);
    if (count == null) return Response.status(404).build();
    return Response.ok(Map.of("word", word, "count", count)).build();
}
```

- Inject `org.apache.kafka.streams.KafkaStreams` directly.
- Use `StoreQueryParameters.fromNameAndType(storeName, storeType)` to access state stores.
- Store types: `QueryableStoreTypes.keyValueStore()`, `.windowStore()`, `.sessionStore()`.

### Windowed Aggregations

```java
builder.<String, String>stream("orders")
    .groupByKey()
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
    .count(Materialized.as(Stores.persistentWindowStore("order-counts", Duration.ofDays(1), Duration.ofMinutes(5), false)))
    .toStream()
    .map((windowedKey, count) -> KeyValue.pair(windowedKey.key(), count))
    .to("order-counts-output", Produced.with(Serdes.String(), Serdes.Long()));
```

### GlobalKTable (Reference Data Joins)

```java
GlobalKTable<String, String> products = builder.globalTable("products",
    Consumed.with(Serdes.String(), Serdes.String()));

builder.<String, String>stream("orders")
    .join(products,
        (orderKey, orderValue) -> extractProductId(orderValue),
        (order, product) -> enrichOrder(order, product))
    .to("enriched-orders");
```

### Testing

- Dev Services starts Kafka automatically.
- Kafka Streams takes several seconds to rebalance and start processing — use `Awaitility` with sufficient timeouts (10-30s).
- To send test data, use `KafkaProducer` directly (inject config with `@Identifier("default-kafka-broker")`).
- Interactive queries may return `InvalidStateStoreException` if the streams app hasn't fully started — retry in tests.

### Common Pitfalls

- Specify serdes in topology code (`Grouped.with(...)`, `Produced.with(...)`, `Consumed.with(...)`) — config-based default serdes may not be picked up.
- `Materialized.as(String)` with type parameters changed in Kafka Streams 4.x — use `Materialized.as(Stores.persistentKeyValueStore(name))` instead.
- The `topics` config property lists topics the app DEPENDS on, not topics it creates — Quarkus waits for them.
- State stores are persisted to disk by default (`/tmp/kafka-streams/`) — data survives restarts in dev mode.
- Hot reload may not work reliably for topology changes — restart the app if the topology is stuck in ERROR state.
