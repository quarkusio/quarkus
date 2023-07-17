package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SimpleService {
    public String hello() {
        return "hello from JavaComponent";
    }
}
