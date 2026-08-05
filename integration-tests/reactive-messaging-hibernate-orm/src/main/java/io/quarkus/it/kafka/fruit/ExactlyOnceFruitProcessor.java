package io.quarkus.it.kafka.fruit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.logging.Log;
import io.quarkus.smallrye.reactivemessaging.kafka.ExactlyOnce;
import io.smallrye.reactive.messaging.kafka.Record;

@ApplicationScoped
public class ExactlyOnceFruitProcessor {

    private final List<String> processed = new CopyOnWriteArrayList<>();
    private volatile boolean failOnce = true;

    @Incoming("exactly-once-fruit-in")
    @Outgoing("exactly-once-fruit-out")
    @ExactlyOnce
    @Transactional
    ProducerRecord<String, String> process(Record<String, String> record) {
        Log.info("Processing record with key: " + record.key() + " and value: " + record.value());
        if ("fail-fruit".equals(record.value()) && failOnce) {
            failOnce = false;
            throw new RuntimeException("Simulated failure for fail-fruit");
        }
        if ("dlq-fruit".equals(record.value())) {
            processed.add(record.value());
            return new ProducerRecord<>("exactly-once-fruit-dlq", 0, record.key(), record.value());
        }
        ExactlyOnceFruit fruit = new ExactlyOnceFruit(record.value());
        fruit.persist();
        processed.add(record.value());
        return new ProducerRecord<>("exactly-once-fruit-out", record.key(), "persisted-" + record.value());
    }

    public List<String> getProcessed() {
        return processed;
    }
}
