package com.example.grpc.hibernate;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;

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
