package io.quarkus.hibernate.orm.panache.deployment.test;

import jakarta.persistence.Id;

public class DuplicateIdWithParentEntity extends DuplicateIdParentEntity {
    @Id
    public String customId;
}
