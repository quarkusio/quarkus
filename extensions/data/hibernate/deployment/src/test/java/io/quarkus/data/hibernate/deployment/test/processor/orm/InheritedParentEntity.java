/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.hibernate.deployment.test.processor.orm;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import io.quarkus.data.hibernate.ManagedEntity;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class InheritedParentEntity extends ManagedEntity {
    public String name;
}
