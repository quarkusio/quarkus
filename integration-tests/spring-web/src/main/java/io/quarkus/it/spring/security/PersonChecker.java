package io.quarkus.it.spring.security;

import org.springframework.stereotype.Component;

@Component
public class PersonChecker {
    public boolean check(String name) {
        return name.equals("correct-name");
    }
}
