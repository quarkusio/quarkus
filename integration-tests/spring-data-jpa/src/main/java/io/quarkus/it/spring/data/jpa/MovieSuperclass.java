package io.quarkus.it.spring.data.jpa;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

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
