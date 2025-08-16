package io.quarkus.it.panache.defaultpu;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PersonNameDoubleConstructor extends PersonName {
    @SuppressWarnings("unused")
    public PersonNameDoubleConstructor(String uniqueName, String name, Object fakeParameter) {
        super(uniqueName, name);
    }

    @SuppressWarnings("unused")
    public PersonNameDoubleConstructor(String uniqueName, String name) {
        super(uniqueName, name);
    }
}