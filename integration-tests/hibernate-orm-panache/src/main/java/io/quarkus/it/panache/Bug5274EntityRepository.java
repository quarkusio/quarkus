package io.quarkus.it.panache;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Bug5274EntityRepository extends AbstractRepository<Person> {
}
