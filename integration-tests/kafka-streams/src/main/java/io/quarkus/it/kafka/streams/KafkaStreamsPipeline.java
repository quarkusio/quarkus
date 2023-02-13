package io.quarkus.it.kafka.streams;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams.State;
import org.apache.kafka.streams.KafkaStreams.StateListener;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;

@ApplicationScoped
public class KafkaStreamsPipeline {

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        ObjectMapperSerde<Category> categorySerde = new ObjectMapperSerde<>(Category.class);
        ObjectMapperSerde<Customer> customerSerde = new ObjectMapperSerde<>(Customer.class);
        ObjectMapperSerde<EnrichedCustomer> enrichedCustomerSerde = new ObjectMapperSerde<>(EnrichedCustomer.class);

        KTable<Integer, Category> categories = builder.table(
                "streams-test-categories",
                Consumed.with(Serdes.Integer(), categorySerde));

        KStream<Integer, EnrichedCustomer> customers = builder
                .stream("streams-test-customers", Consumed.with(Serdes.Integer(), customerSerde))
                .selectKey((id, customer) -> customer.category)
                .join(
                        categories,
                        (customer, category) -> {
                            return new EnrichedCustomer(customer.id, customer.name, category);
                        },
                        Joined.with(Serdes.Integer(), customerSerde, categorySerde));

        KeyValueBytesStoreSupplier storeSupplier = Stores.inMemoryKeyValueStore("countstore");
        customers.groupByKey()
                .count(Materialized.<Integer, Long> as(storeSupplier));

        customers.selectKey((categoryId, customer) -> customer.id)
                .to("streams-test-customers-processed", Produced.with(Serdes.Integer(), enrichedCustomerSerde));

        return builder.build();
    }

    @Produces
    @Singleton
    CurrentStateListener stateListener() {
        return new CurrentStateListener();
    }

    class CurrentStateListener implements StateListener {
        State currentState;

        @Override
        public void onChange(State newState, State oldState) {
            this.currentState = newState;
        }
    }
}
