package io.quarkus.it.mockbean;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class AlphaObserver {

    public boolean test() {
        return true;
    }

    void onBigDecimal(@Observes AtomicReference<BigDecimal> event) {
        event.set(event.get().add(BigDecimal.ONE));
    }

}
