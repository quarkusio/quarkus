package io.quarkus.it.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

@ApplicationScoped
public class KafkaConsumerManager {

    public static KafkaConsumer<Integer, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("test-consumer"));
        return consumer;
    }

    @Inject
    Event<Consumer<?, ?>> consumerEvent;

    private Consumer<Integer, String> consumer;

    @PostConstruct
    public void create() {
        consumer = createConsumer();
        consumerEvent.fire(consumer);
    }

    @PreDestroy
    public void cleanup() {
        consumerEvent.fire(consumer);
    }

    public String receive() {
        final ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofMillis(60000));
        if (records.isEmpty()) {
            return null;
        }
        return records.iterator().next().value();
    }

}
