package io.vertx.axle.kafka;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.kafka.client.consumer.KafkaConsumer;
import io.vertx.axle.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.axle.kafka.client.producer.KafkaProducer;
import io.vertx.axle.kafka.client.producer.KafkaProducerRecord;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaClientTest {

    @Rule
    public KafkaContainer container = new KafkaContainer();

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @After
    public void tearDown() {
        vertx.close();
    }

    @Test
    public void testAxleAPI() {
        Map<String, String> configOfTheConsumer = new HashMap<>();
        configOfTheConsumer.put("bootstrap.servers", container.getBootstrapServers());
        configOfTheConsumer.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        configOfTheConsumer.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        configOfTheConsumer.put("group.id", "my_group");
        configOfTheConsumer.put("auto.offset.reset", "earliest");
        configOfTheConsumer.put("enable.auto.commit", "false");

        Map<String, String> configOfProducer = new HashMap<>();
        configOfProducer.put("bootstrap.servers", container.getBootstrapServers());
        configOfProducer.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        configOfProducer.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        configOfProducer.put("acks", "1");

        String uuid = UUID.randomUUID().toString();
        KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, configOfTheConsumer,
                String.class, String.class);
        CompletionStage<Optional<String>> result = ReactiveStreams.fromPublisher(consumer.toPublisher()).map(KafkaConsumerRecord::value)
                .findFirst()
                .run();
        consumer.subscribe("my-topic").toCompletableFuture().join();

        KafkaProducer<String, String> producer = KafkaProducer.create(vertx, configOfProducer);
        producer.write(KafkaProducerRecord.create("my-topic", uuid));

        Optional<String> optional = result.toCompletableFuture().join();
        assertThat(optional).contains(uuid);
    }
}
