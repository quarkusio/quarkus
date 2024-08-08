package io.quarkus.it.panache.defaultpu;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Bug5274EntityRepository extends AbstractRepository<Person> {
}
