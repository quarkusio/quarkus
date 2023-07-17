package io.quarkus.arc.test.unused;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class ProducerBean {

    @Produces
    @ApplicationScoped
    Delta produceDelta() {
        return new Delta("ok");
    }
}
