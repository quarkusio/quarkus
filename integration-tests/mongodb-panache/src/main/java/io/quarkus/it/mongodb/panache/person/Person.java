package io.quarkus.it.mongodb.panache.person;

import org.bson.codecs.pojo.annotations.BsonId;

public class Person {
    @BsonId
    public Long id;
    public String firstname;
    public String lastname;
    public Status status = Status.ALIVE;
}
