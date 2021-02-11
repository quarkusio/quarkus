package io.quarkus.spring.security.deployment.springapp;

import org.springframework.stereotype.Component;

@Component
public class PrincipalChecker {

    public boolean isSame(String input, String username) {
        return input.equals(username);
    }
}
