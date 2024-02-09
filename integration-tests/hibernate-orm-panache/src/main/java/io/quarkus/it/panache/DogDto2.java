package io.quarkus.it.panache;

import io.quarkus.hibernate.orm.panache.common.NestedProjectedClass;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DogDto2 {
    public String name;
    public PersonDto2 owner;

    public DogDto2(String name, PersonDto2 owner) {
        this.name = name;
        this.owner = owner;
    }

    @NestedProjectedClass
    public static class PersonDto2 {
        public String name;

        public PersonDto2(String name) {
            this.name = name;
        }
    }
}
