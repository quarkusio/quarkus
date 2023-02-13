package org.acme.service;

import org.jetbrains.annotations.NotNull;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SimpleService {
    @NotNull
    public String hello() {
        return "hello from JavaComponent";
    }
}
