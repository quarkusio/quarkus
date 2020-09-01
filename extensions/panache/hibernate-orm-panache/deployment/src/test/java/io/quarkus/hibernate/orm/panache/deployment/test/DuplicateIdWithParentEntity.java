package io.quarkus.hibernate.orm.panache.deployment.test;

import javax.persistence.Id;

public class DuplicateIdWithParentEntity extends DuplicateIdParentEntity {
    @Id
    public String customId;
}
