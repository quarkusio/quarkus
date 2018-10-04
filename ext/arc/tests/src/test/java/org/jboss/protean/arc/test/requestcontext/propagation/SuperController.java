package org.jboss.protean.arc.test.requestcontext.propagation;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

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
