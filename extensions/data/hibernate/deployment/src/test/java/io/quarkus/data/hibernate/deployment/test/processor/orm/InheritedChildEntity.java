package io.quarkus.data.hibernate.deployment.test.processor.orm;

import jakarta.persistence.Entity;

@Entity
public class InheritedChildEntity extends InheritedParentEntity {
    public String extra;
}
