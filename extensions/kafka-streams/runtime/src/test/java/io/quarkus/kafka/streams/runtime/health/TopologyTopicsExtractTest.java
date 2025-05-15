package io.quarkus.kafka.streams.runtime.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.enterprise.inject.Produces;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.junit.jupiter.api.Test;

import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;

public class TopologyTopicsExtractTest {

    @Test
    void testTopologyWithStateStore() {
        Set<String> topics = new HashSet<>();
        Set<Pattern> patterns = new HashSet<>();
        KafkaStreamsTopologyManager.extractSources(topologyWithStateStore(), topics, patterns);
        assertThat(topics).containsExactlyInAnyOrder("WEATHER_STATIONS_TOPIC", "TEMPERATURE_VALUES_TOPIC");
    }

    @Test
    void testTopologyWithSelectKey() {
        Set<String> topics = new HashSet<>();
        Set<Pattern> patterns = new HashSet<>();
        KafkaStreamsTopologyManager.extractSources(buildTopology(), topics, patterns);
        assertThat(topics).containsExactlyInAnyOrder("streams-test-customers", "streams-test-categories");
    }

    public Topology topologyWithStateStore() {
        StreamsBuilder builder = new StreamsBuilder();

        KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore(
                "WEATHER_STATIONS_STORE");

        GlobalKTable<Integer, String> stations = builder.globalTable( // <1>
                "WEATHER_STATIONS_TOPIC",
                Consumed.with(Serdes.Integer(), Serdes.String()));

        builder.stream( // <2>
                "TEMPERATURE_VALUES_TOPIC",
                Consumed.with(Serdes.Integer(), Serdes.String()))
                .join( // <3>
                        stations,
                        (stationId, timestampAndValue) -> stationId,
                        (timestampAndValue, station) -> {
                            String[] parts = timestampAndValue.split(";");
                            return parts[0] + "," + parts[1] + "," + station;
                        })
                .groupByKey() // <4>
                .aggregate( // <5>
                        String::new,
                        (stationId, value, aggregation) -> aggregation + value,
                        Materialized.<Integer, String> as(storeSupplier)
                                .withKeySerde(Serdes.Integer())
                                .withValueSerde(Serdes.String()))
                .toStream()
                .to( // <6>
                        "TEMPERATURES_AGGREGATED_TOPIC",
                        Produced.with(Serdes.Integer(), Serdes.String()));

        return builder.build();
    }

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        KTable<Integer, String> categories = builder.table(
                "streams-test-categories",
                Consumed.with(Serdes.Integer(), Serdes.String()));

        KStream<Integer, String> customers = builder
                .stream("streams-test-customers", Consumed.with(Serdes.Integer(), Serdes.String()))
                .selectKey((id, customer) -> customer.length())
                .join(categories,
                        (customer, category) -> "" + customer.length() + category.length(),
                        Joined.with(Serdes.Integer(), Serdes.String(), Serdes.String()));

        KeyValueBytesStoreSupplier storeSupplier = Stores.inMemoryKeyValueStore("countstore");
        customers.groupByKey()
                .count(Materialized.as(storeSupplier));

        customers.selectKey((categoryId, customer) -> customer)
                .to("streams-test-customers-processed", Produced.with(Serdes.String(), Serdes.String()));

        return builder.build();
    }
}
