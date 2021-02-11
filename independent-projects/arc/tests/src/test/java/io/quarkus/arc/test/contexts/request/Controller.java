package io.quarkus.arc.test.contexts.request;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;

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
