package io.quarkus.it.spring.data.jpa;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public abstract class MovieSuperclass {

    @Id
    protected Long id;

    @Version
    protected Long version;

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }
}
