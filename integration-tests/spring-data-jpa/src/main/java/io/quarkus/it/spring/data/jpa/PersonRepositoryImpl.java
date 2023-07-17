package io.quarkus.it.spring.data.jpa;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class PersonRepositoryImpl implements PersonFragment, PersonFragment2 {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public List<Person> findAll() {
        return entityManager.createQuery("SELECT p FROM Person p").getResultList();
    }

    @Override
    public void makeNameUpperCase(Person person) {
        person.setName(person.getName().toUpperCase());
    }

    @Override
    public void doNothing() {

    }
}
