package io.quarkus.it.spring.web;

import org.springframework.stereotype.Service;

@Service
public class GreetingService {

    public Greeting greet(String message) {
        return new Greeting(message);
    }
}
