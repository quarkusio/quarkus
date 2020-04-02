package io.quarkus.it.mockbean;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CapitalizerService {

    public String capitalize(String input) {
        return input.toUpperCase();
    }
}
