package io.quarkus.it.corestuff.serialization;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(serialization = true)
public class Person extends AbstractPerson {
    public Person() {
    }

    public Person(String name) {
        setName(name);
    }
}
