package io.quarkus.it.panache.reactive;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PersonName {
    public final String name;
    public final String uniqueName;

    public PersonName(String uniqueName, String name) {
        this.name = name;
        this.uniqueName = uniqueName;
    }
}
