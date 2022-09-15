package io.quarkus.it.jpa.h2;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Id;

/**
 * This entity isn't directly referenced: its mere presence is
 * useful to be able to verify bootstrap capabilities in the
 * presence of abstract entities in the hierarchy.
 */
public class IdVersionPK implements Serializable {
    @Id
    private String id;

    @Id
    private Long version;

    public IdVersionPK() {
    }

    public IdVersionPK(String id, Long version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IdVersionPK that = (IdVersionPK) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

}
