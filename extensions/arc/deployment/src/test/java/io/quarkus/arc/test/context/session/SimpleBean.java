package io.quarkus.arc.test.context.session;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;

@SessionScoped
class SimpleBean {

    static final AtomicBoolean DESTROYED = new AtomicBoolean();

    private String id;

    @PostConstruct
    void init() {
        id = UUID.randomUUID().toString();
    }

    public String ping() {
        return id;
    }

    @PreDestroy
    void destroy() {
        DESTROYED.set(true);
    }
}