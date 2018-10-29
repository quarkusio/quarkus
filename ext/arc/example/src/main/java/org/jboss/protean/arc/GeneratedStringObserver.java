package org.jboss.protean.arc;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GeneratedStringObserver {

    @Inject
    @ConfigProperty(name = "logEvery", defaultValue = "1000")
    Integer logEvery;

    private final AtomicInteger COUNT = new AtomicInteger(0);

    void stringGenerated(@Observes @Generated String generated) {
        if (COUNT.incrementAndGet() % logEvery == 0) {
            System.out.println("Generated " + COUNT.get() + " strings");
        }
    }

}
