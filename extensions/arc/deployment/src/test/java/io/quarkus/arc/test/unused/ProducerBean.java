package io.quarkus.arc.test.unused;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

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
