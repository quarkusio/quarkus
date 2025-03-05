package com.example;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyBean {
    public String hello() {
        return "hello";
    }
}
