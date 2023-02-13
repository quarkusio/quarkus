package io.quarkus.it.panache;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Bug5885EntityRepository extends Bug5885AbstractRepository<Person> {
}
