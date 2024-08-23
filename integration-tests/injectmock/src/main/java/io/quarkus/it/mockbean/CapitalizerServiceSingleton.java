package io.quarkus.it.mockbean;

import jakarta.inject.Singleton;

@Singleton
public class CapitalizerServiceSingleton {

    public String capitalize(String input) {
        return input.toUpperCase();
    }
}
