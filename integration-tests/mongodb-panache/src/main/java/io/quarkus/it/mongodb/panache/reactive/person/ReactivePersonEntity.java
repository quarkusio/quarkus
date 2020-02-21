package io.quarkus.it.mongodb.panache.reactive.person;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;

public class ReactivePersonEntity extends ReactivePanacheMongoEntityBase {
    @BsonId
    public Long id;
    public String firstname;
    public String lastname;
}
