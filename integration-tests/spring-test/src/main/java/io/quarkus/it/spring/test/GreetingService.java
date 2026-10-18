package io.quarkus.it.spring.test;

import jakarta.inject.Singleton;

@Singleton
public class GreetingService {

    public String greet(String name) {
        return "hello " + name;
    }
}
