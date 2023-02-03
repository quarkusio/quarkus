package io.quarkus.arc.test.lifecyclecallbacks.inherited;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OriginalBean {

    static final AtomicBoolean POST_CONSTRUCT = new AtomicBoolean();
    static final AtomicBoolean PRE_DESTROY = new AtomicBoolean();

    // package private, not visible from AlternativeBean without reflection access
    @PostConstruct
    void postConstruct() {
        POST_CONSTRUCT.set(true);
    }

    @PreDestroy
    void preDestroy() {
        PRE_DESTROY.set(true);
    }

    public String ping() {
        return OriginalBean.class.getSimpleName();
    }

}
