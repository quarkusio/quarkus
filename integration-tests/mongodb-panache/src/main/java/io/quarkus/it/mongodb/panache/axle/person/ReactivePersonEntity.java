package io.quarkus.it.mongodb.panache.axle.person;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.axle.ReactivePanacheMongoEntityBase;

public class ReactivePersonEntity extends ReactivePanacheMongoEntityBase {
    @BsonId
    public Long id;
    public String firstname;
    public String lastname;
}
