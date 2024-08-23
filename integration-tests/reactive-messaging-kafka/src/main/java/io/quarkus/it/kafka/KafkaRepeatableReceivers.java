package io.quarkus.it.kafka;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

@ApplicationScoped
public class KafkaRepeatableReceivers {

    private final List<Double> prices = new CopyOnWriteArrayList<>();

    @Incoming("prices-in")
    @Incoming("prices-in2")
    public CompletionStage<Void> consume(Message<Double> msg) {
        prices.add(msg.getPayload());
        return msg.ack();
    }

    public List<Double> getPrices() {
        return prices;
    }

}
