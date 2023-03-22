package io.quarkus.it.kafka;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class PricesProducer {

    @Outgoing("prices-out")
    public Multi<Double> generatePrices() {
        return Multi.createFrom().items(1.2, 2.2, 3.4);
    }

    @Outgoing("prices-out2")
    public Multi<Double> generatePrices2() {
        return Multi.createFrom().items(4.5, 5.6, 6.7);
    }
}
