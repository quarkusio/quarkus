package io.quarkus.it.spring.data.jpa;

import java.util.List;

public interface PersonFragment {

    // custom findAll
    List<Person> findAll();

    void makeNameUpperCase(Person person);

    default void doNothingMore() {

    }
}
