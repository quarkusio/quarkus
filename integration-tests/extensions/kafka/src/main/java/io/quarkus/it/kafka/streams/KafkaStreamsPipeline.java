package io.quarkus.it.kafka.streams;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.kafka.common.serialization.Serdes;
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

import io.quarkus.kafka.client.serialization.JsonbSerde;

@ApplicationScoped
public class KafkaStreamsPipeline {

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        JsonbSerde<Category> categorySerde = new JsonbSerde<>(Category.class);
        JsonbSerde<Customer> customerSerde = new JsonbSerde<>(Customer.class);
        JsonbSerde<EnrichedCustomer> enrichedCustomerSerde = new JsonbSerde<>(EnrichedCustomer.class);

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
}
