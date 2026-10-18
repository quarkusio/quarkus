package com.example.reactive;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;

import io.smallrye.common.vertx.ContextLocals;

@RequestScoped
public class RequestScopeBean {
    private static final AtomicInteger idSequence = new AtomicInteger();
    private int id;

    @PostConstruct
    public void setUp() {
        id = idSequence.getAndIncrement();
        ContextLocals.put("context-id", id);
    }

    public int getId() {
        return id;
    }
}
