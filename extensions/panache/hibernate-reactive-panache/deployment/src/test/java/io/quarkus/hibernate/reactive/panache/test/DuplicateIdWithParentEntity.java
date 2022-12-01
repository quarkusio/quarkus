package io.quarkus.hibernate.reactive.panache.test;

import javax.persistence.Id;

public class DuplicateIdWithParentEntity extends DuplicateIdParentEntity {
    @Id
    public String customId;
}
