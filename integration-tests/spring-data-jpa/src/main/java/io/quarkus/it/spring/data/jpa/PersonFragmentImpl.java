package io.quarkus.it.spring.data.jpa;

import java.util.List;

import org.springframework.stereotype.Component;

import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;

@Component
public class PersonFragmentImpl implements PersonFragment, PersonFragment2 {

    @Override
    public List<Person> findAll() {
        System.out.println("Custom findAll");
        return (List<Person>) JpaOperations.findAll(Person.class).list();
    }

    @Override
    public void makeNameUpperCase(Person person) {
        person.setName(person.getName().toUpperCase());
    }

    @Override
    public void doNothing() {

    }
}
