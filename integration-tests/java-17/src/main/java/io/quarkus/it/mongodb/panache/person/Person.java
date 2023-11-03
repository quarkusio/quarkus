package io.quarkus.it.mongodb.panache.person;

import io.quarkus.mongodb.panache.PanacheMongoEntity;

public class Person extends PanacheMongoEntity {
    public String firstname;
    public String lastname;
    public Status status = Status.ALIVE;
}
