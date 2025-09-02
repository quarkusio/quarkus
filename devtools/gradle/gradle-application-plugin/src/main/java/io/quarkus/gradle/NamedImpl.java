package io.quarkus.gradle;

import java.util.Objects;

import org.gradle.api.Named;

class NamedImpl implements Named {

    private final String name;

    public NamedImpl(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String getName() {
        return name;
    }
}
