/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.hibernate.deployment.test.processor.orm;

import jakarta.persistence.Entity;

@Entity
public class InheritedChildEntity extends InheritedParentEntity {
    public String extra;
}
