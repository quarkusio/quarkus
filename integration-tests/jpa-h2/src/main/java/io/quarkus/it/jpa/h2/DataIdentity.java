package io.quarkus.it.jpa.h2;

import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;

/**
 * This entity isn't directly referenced: its mere presence is
 * useful to be able to verify bootstrap capabilities in the
 * presence of abstract entities in the hierarchy.
 */
@MappedSuperclass
@IdClass(IdVersionPK.class)
public abstract class DataIdentity {
    @Id
    private String id;

    @Id
    private Long version;

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
}
