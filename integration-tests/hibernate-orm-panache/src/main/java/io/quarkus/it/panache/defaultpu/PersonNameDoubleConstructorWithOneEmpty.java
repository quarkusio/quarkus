package io.quarkus.it.panache.defaultpu;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PersonNameDoubleConstructorWithOneEmpty extends PersonName {
    @SuppressWarnings("unused")
    public PersonNameDoubleConstructorWithOneEmpty() {
        super(null, null);
    }

    @SuppressWarnings("unused")
    public PersonNameDoubleConstructorWithOneEmpty(String uniqueName, String name) {
        super(uniqueName, name);
    }
}
