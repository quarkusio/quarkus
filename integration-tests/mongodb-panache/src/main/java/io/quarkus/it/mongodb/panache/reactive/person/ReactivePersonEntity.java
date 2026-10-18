package io.quarkus.it.mongodb.panache.reactive.person;

import java.util.List;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

public class ReactivePersonEntity extends ReactivePanacheMongoEntityBase {
    @BsonId
    public Long id;
    public String firstname;
    public String lastname;

    public static Uni<List<ReactivePersonEntity>> findOrdered() {
        return findAll(Sort.by("lastname", "firstname")).list();
    }
}
