package io.quarkus.it.panache;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Bug5274EntityRepository extends AbstractRepository<Person> {
}
