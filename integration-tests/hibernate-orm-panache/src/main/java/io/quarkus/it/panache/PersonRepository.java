package io.quarkus.it.panache;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class PersonRepository implements PanacheRepository<Person> {
    public List<Person> findOrdered() {
        return find("ORDER BY name").list();
    }
}
