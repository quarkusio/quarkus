package io.quarkus.arc.test.contexts.request.propagation;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;

@Dependent
public class SuperButton {

    static final AtomicBoolean DESTROYED = new AtomicBoolean();

    @PreDestroy
    void destroy() {
        DESTROYED.set(true);
    }

}
