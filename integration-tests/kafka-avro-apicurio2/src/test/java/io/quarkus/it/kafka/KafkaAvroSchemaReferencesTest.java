package io.quarkus.it.kafka;

import static io.apicurio.registry.serde.avro.AvroSerdeConfig.USE_SPECIFIC_AVRO_READER;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.Test;

import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import io.quarkus.it.kafka.avro.Order;
import io.quarkus.it.kafka.avro.OrderStatus;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

/**
 * Tests Avro schemas with references (e.g., a record referencing an enum type).
 * This covers the scenario from <a href="https://github.com/quarkusio/quarkus/issues/42504">#42504</a>
 * where Avro schemas with references caused AvroTypeException after the Avro 1.12 upgrade.
 */
@QuarkusTest
@WithTestResource(value = KafkaCompanionResource.class)
public class KafkaAvroSchemaReferencesTest {

    private static final String ORDER_TOPIC = "test-avro-order-references";

    @InjectKafkaCompanion
    KafkaCompanion kafkaCompanion;

    @Test
    public void testAvroSchemaWithEnumReference() {
        Map<String, Object> config = new HashMap<>(kafkaCompanion.getCommonClientConfig());
        config.put(USE_SPECIFIC_AVRO_READER, "true");
        config.put("apicurio.registry.id-handler", "io.apicurio.registry.serde.Legacy8ByteIdHandler");

        Serde<Order> orderSerde = Serdes.serdeFrom(
                new AvroKafkaSerializer<>(),
                new AvroKafkaDeserializer<>());
        orderSerde.configure(config, false);
        kafkaCompanion.registerSerde(Order.class, orderSerde);

        kafkaCompanion.produce(Order.class)
                .fromRecords(
                        new org.apache.kafka.clients.producer.ProducerRecord<>(ORDER_TOPIC, "key1",
                                createOrder("order-1", "Laptop", 2, OrderStatus.PENDING)),
                        new org.apache.kafka.clients.producer.ProducerRecord<>(ORDER_TOPIC, "key2",
                                createOrder("order-2", "Phone", 1, OrderStatus.SHIPPED)),
                        new org.apache.kafka.clients.producer.ProducerRecord<>(ORDER_TOPIC, "key3",
                                createOrder("order-3", "Tablet", 3, OrderStatus.DELIVERED)));

        ConsumerTask<String, Order> consumer = kafkaCompanion.consume(Order.class)
                .withGroupId("test-group-order-" + UUID.randomUUID())
                .withOffsetReset(OffsetResetStrategy.EARLIEST)
                .fromTopics(ORDER_TOPIC);

        List<ConsumerRecord<String, Order>> received = consumer.awaitRecords(3, Duration.ofSeconds(10L)).getRecords();

        await().atMost(10, SECONDS).until(() -> received.size() >= 3);

        List<String> products = received.stream().map(r -> r.value().getProduct()).toList();
        assertThat(products, hasItems("Laptop", "Phone", "Tablet"));

        List<OrderStatus> statuses = received.stream().map(r -> r.value().getStatus()).toList();
        assertThat(statuses, hasItems(OrderStatus.PENDING, OrderStatus.SHIPPED, OrderStatus.DELIVERED));
    }

    private Order createOrder(String id, String product, int quantity, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setProduct(product);
        order.setQuantity(quantity);
        order.setStatus(status);
        return order;
    }
}
