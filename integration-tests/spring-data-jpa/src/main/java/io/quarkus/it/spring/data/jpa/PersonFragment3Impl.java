package io.quarkus.it.spring.data.jpa;

public class PersonFragment3Impl implements PersonFragment3 {

    @Override
    public String getName(Person person) {
        return person.getName();
    }
}
