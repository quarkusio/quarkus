package io.quarkus.it.spring.data.jpa;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class Animal {

    @Id
    @GeneratedValue
    public long id;

    public long getId() {
        return id;
    }
}
