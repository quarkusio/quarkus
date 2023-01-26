package io.quarkus.it.mockbean;

import javax.inject.Singleton;

@Singleton
public class CapitalizerService {

    public String capitalize(String input) {
        return input.toUpperCase();
    }
}
