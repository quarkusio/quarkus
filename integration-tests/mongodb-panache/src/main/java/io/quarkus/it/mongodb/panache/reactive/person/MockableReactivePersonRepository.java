package io.quarkus.it.mongodb.panache.reactive.person;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MockableReactivePersonRepository implements ReactivePanacheMongoRepositoryBase<ReactivePersonEntity, Long> {
    public Uni<List<ReactivePersonEntity>> findOrdered() {
        return findAll(Sort.by("lastname", "firstname")).list();
    }
}
