package io.quarkus.it.spring.data.jpa;

import javax.inject.Singleton;

@Singleton
public class PersonFragment3Impl implements PersonFragment3 {

    @Override
    public String getName(Person person) {
        return person.getName();
    }
}
