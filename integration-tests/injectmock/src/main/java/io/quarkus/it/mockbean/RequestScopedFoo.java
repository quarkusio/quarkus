package io.quarkus.it.mockbean;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;

import io.quarkus.arc.Unremovable;

@Unremovable
@RequestScoped
public class RequestScopedFoo {

    static final AtomicBoolean CONSTRUCTED = new AtomicBoolean();

    public String ping() {
        return "bar";
    }

    @PostConstruct
    void init() {
        CONSTRUCTED.set(true);
    }

}
