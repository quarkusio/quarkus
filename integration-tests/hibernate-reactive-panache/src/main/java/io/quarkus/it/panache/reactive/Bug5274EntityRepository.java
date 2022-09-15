package io.quarkus.it.panache.reactive;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Bug5274EntityRepository extends AbstractRepository<Person> {
}
