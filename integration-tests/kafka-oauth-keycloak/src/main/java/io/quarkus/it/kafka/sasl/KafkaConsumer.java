package io.quarkus.it.kafka.sasl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KafkaConsumer {

    private final Logger log = Logger.getLogger(KafkaConsumer.class);

    private final List<String> list = new CopyOnWriteArrayList<>();

    @Incoming("in")
    public void consume(String value) {
        log.info(value);
        list.add(value);
    }

    public List<String> getValues() {
        return list;
    }
}
