package io.quarkus.it.mongodb.panache.record;

import io.quarkus.mongodb.panache.PanacheMongoEntity;

public class PersonWithRecord extends PanacheMongoEntity {
    public String firstname;
    public String lastname;
    public Status status = Status.ALIVE;
}
