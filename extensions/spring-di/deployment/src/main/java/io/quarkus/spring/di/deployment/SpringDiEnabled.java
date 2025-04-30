package io.quarkus.spring.di.deployment;

import java.util.function.BooleanSupplier;

public class SpringDiEnabled implements BooleanSupplier {

    private final SpringDiBuildTimeConfig config;

    public SpringDiEnabled(SpringDiBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }
}
