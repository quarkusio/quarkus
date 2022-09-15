package io.quarkus.arc.test.contexts.request.propagation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@RequestScoped
public class SuperController {

    static final AtomicBoolean DESTROYED = new AtomicBoolean();

    private String id;

    @Inject
    SuperButton button;

    @PostConstruct
    void init() {
        id = UUID.randomUUID().toString();
    }

    @PreDestroy
    void destroy() {
        DESTROYED.set(true);
    }

    String getId() {
        return id;
    }

    SuperButton getButton() {
        return button;
    }

}
