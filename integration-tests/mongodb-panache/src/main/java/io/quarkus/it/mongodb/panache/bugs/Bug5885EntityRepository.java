package io.quarkus.it.mongodb.panache.bugs;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.it.mongodb.panache.person.PersonEntity;

@ApplicationScoped
public class Bug5885EntityRepository extends Bug5885AbstractRepository<PersonEntity> {
}
