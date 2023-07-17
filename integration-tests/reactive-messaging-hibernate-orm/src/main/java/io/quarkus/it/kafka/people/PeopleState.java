package io.quarkus.it.kafka.people;

import jakarta.persistence.Entity;

import io.quarkus.smallrye.reactivemessaging.kafka.CheckpointEntity;

@Entity
public class PeopleState extends CheckpointEntity {

    String names;

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }
}
