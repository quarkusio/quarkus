package io.quarkus.it.data.hibernate;

import io.quarkus.data.hibernate.ManagedRepository;

@Logged
public interface InterceptedPersonRepository extends ManagedRepository<Person> {

    default String interceptedMethod(String input) {
        return input;
    }
}
