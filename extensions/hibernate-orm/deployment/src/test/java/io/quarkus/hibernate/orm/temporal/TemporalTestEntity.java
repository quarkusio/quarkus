package io.quarkus.hibernate.orm.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.Temporal;

@Temporal(rowStart = "valid_from", rowEnd = "valid_to")
@Entity(name = "TemporalTestEntity")
public class TemporalTestEntity {

    @Id
    long id;

    @Version
    int version;

    String name;

    public TemporalTestEntity() {
    }

    public TemporalTestEntity(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
