package io.quarkus.it.mongodb.panache.person;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.panache.common.Sort;

@ApplicationScoped
public class MockablePersonRepository implements PanacheMongoRepositoryBase<PersonEntity, Long> {
    public List<PersonEntity> findOrdered() {
        return findAll(Sort.by("lastname", "firstname")).list();
    }
}
