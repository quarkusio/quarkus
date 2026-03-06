package io.quarkus.it.kafka.streams.norocksdb;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;

/**
 * A Kafka Streams topology that uses exclusively in-memory state stores.
 * This topology must work correctly when {@code quarkus.kafka-streams.rocksdb.enabled=false}.
 */
@ApplicationScoped
public class InMemoryPipeline {

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        KeyValueBytesStoreSupplier storeSupplier = Stores.inMemoryKeyValueStore("word-count-store");

        // Simple word-count: read strings, split into words, count occurrences
        builder.stream("no-rocksdb-input", Consumed.with(Serdes.String(), Serdes.String()))
                .flatMapValues(value -> java.util.Arrays.asList(value.toLowerCase().split("\\W+")))
                .groupBy((key, word) -> word, Grouped.with(Serdes.String(), Serdes.String()))
                .count(Materialized.<String, Long> as(storeSupplier)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()))
                .toStream()
                .to("no-rocksdb-output", Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }
}
