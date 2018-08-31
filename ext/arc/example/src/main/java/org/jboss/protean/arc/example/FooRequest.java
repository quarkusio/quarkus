package org.jboss.protean.arc.example;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class FooRequest {

    private Bar bar;

    private String id;

    @PostConstruct
    void init() {
        this.id = UUID.randomUUID().toString();
    }

    @Inject
    void setBar(Bar bar) {
        this.bar = bar;
    }

    String getId() {
        return id;
    }

    String ping() {
        return bar.getName();
    }

}
