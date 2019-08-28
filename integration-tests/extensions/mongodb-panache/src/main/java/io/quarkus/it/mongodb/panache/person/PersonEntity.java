package io.quarkus.it.mongodb.panache.person;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;

public class PersonEntity extends PanacheMongoEntityBase {
    @BsonId
    public Long id;
    public String firstname;
    public String lastname;
}
