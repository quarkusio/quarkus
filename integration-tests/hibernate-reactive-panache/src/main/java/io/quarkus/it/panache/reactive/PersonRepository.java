package io.quarkus.it.panache.reactive;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class PersonRepository implements PanacheRepository<Person> {
    public Uni<List<Person>> findOrdered() {
        return find("ORDER BY name").list();
    }
}
