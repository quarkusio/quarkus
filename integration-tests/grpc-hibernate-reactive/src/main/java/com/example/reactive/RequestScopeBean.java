package com.example.reactive;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestScopeBean {
    private static final AtomicInteger idSequence = new AtomicInteger();
    private int id;

    @PostConstruct
    public void setUp() {
        id = idSequence.getAndIncrement();
    }

    public int getId() {
        return id;
    }
}
