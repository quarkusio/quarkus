package io.quarkus.it.mockbean;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CapitalizerService {

    public String capitalize(String input) {
        return input.toUpperCase();
    }
}
