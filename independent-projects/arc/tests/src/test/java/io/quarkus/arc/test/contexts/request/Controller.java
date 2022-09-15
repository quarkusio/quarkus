package io.quarkus.arc.test.contexts.request;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@RequestScoped
public class Controller {

    static final AtomicBoolean DESTROYED = new AtomicBoolean();

    private String id;

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
}
