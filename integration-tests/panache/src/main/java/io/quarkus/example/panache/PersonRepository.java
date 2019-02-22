package io.quarkus.example.panache;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.panache.jpa.PanacheRepository;

@ApplicationScoped
public class PersonRepository implements PanacheRepository<Person> {
}
