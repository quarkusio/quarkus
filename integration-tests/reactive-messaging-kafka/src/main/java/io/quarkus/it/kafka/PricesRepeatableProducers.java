package io.quarkus.it.kafka;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.Targeted;

@ApplicationScoped
public class PricesRepeatableProducers {

    @Outgoing("prices-out")
    @Outgoing("prices-out2")
    public Multi<Targeted> produce() {
        return Multi.createFrom().items(
                Targeted.of("prices-out", 1.2, "prices-out2", 4.5),
                Targeted.of("prices-out", 2.2, "prices-out2", 5.6),
                Targeted.of("prices-out", 3.4, "prices-out2", 6.7));
    }

}
