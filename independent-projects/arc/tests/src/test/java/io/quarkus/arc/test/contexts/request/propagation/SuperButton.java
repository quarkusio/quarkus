package io.quarkus.arc.test.contexts.request.propagation;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import java.util.concurrent.atomic.AtomicBoolean;

@Dependent
public class SuperButton {

    static final AtomicBoolean DESTROYED = new AtomicBoolean();

    @PreDestroy
    void destroy() {
        DESTROYED.set(true);
    }

}
