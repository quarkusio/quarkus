package io.quarkus.hibernate.reactive.panache.test;

import jakarta.persistence.Id;

public class DuplicateIdWithParentEntity extends DuplicateIdParentEntity {
    @Id
    public String customId;
}
