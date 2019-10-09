package io.quarkus.it.spring.data.jpa;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class MovieSuperclass {

    @Id
    @GeneratedValue
    private Long id;

    public Long getId() {
        return id;
    }
}
