package io.quarkus.it.spring.security;

import org.springframework.stereotype.Component;

@Component
public class AlwaysFalseChecker {

    public boolean check(String input) {
        return false;
    }
}
