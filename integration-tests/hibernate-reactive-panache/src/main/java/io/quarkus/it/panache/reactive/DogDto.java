package io.quarkus.it.panache.reactive;

import io.quarkus.hibernate.reactive.panache.common.NestedProjectedClass;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DogDto {
    public String name;
    public PersonDto owner;

    public DogDto(String name, PersonDto owner) {
        this.name = name;
        this.owner = owner;
    }

    @NestedProjectedClass
    public static class PersonDto {
        public String name;

        public PersonDto(String name) {
            this.name = name;
        }
    }
}
