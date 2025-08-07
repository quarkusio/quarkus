package io.quarkus.it.kafka;

import static io.apicurio.registry.serde.avro.AvroKafkaSerdeConfig.USE_SPECIFIC_AVRO_READER;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import io.quarkus.it.kafka.avro.Pet;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

@QuarkusTest
@WithTestResource(value = KafkaCompanionResource.class)
public class KafkaAvroWithKafkaCompanionTest {

    @InjectKafkaCompanion
    KafkaCompanion kafkaCompanion;

    @Test
    public void consumeWithCompanion() {
        kafkaCompanion.getCommonClientConfig().put(USE_SPECIFIC_AVRO_READER, "true");
        Serde<Pet> avroSerde = Serdes.serdeFrom(new AvroKafkaSerializer<>(), new AvroKafkaDeserializer<>());
        avroSerde.configure(kafkaCompanion.getCommonClientConfig(), false);
        kafkaCompanion.registerSerde(Pet.class, avroSerde);

        given().contentType(ContentType.JSON)
                .body("{\"name\":\"Fluffy\",\"color\":\"ginger\"}")
                .when()
                .post("/avro/companion")
                .then()
                .statusCode(204);

        given().contentType(ContentType.JSON)
                .body("{\"name\":\"Fang\",\"color\":\"black\"}")
                .when()
                .post("/avro/companion")
                .then()
                .statusCode(204);

        ConsumerTask<String, Pet> consumer = kafkaCompanion.consume(Pet.class)
                .withGroupId("test-group-" + UUID.randomUUID())
                .withOffsetReset(OffsetResetStrategy.EARLIEST)
                .fromTopics("test-avro-apicurio-companion-producer");

        List<ConsumerRecord<String, Pet>> received = consumer.awaitRecords(2, Duration.ofSeconds(5L)).getRecords();
        // check if, after at most 5 seconds, we have at least 2 items collected, and they are what we expect
        await().atMost(5, SECONDS).until(() -> received.size() >= 2);
        List<String> pets = received.stream().map(r -> r.value().getName()).toList();
        assertThat(pets, Matchers.hasItems("Fluffy", "Fang"));
    }
}
