package io.quarkus.it.kafka;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

@ApplicationScoped
public class KafkaProducerManager {

    public static Producer<Integer, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "test");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<Integer, String>(props);
    }

    @Inject
    Event<Producer<?, ?>> producerEvent;

    private int count;
    private Producer<Integer, String> producer;

    @PostConstruct
    public void create() {
        producer = createProducer();
        producerEvent.fire(producer);
    }

    @PreDestroy
    public void cleanup() {
        producerEvent.fire(producer);
    }

    public void send(String message) {
        producer.send(new ProducerRecord<>("test", count++, message));
        producer.flush();
    }

}
