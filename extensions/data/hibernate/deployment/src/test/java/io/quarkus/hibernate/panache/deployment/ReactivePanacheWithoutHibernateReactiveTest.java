/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.hibernate.panache.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.Capabilities;
import io.quarkus.hibernate.panache.deployment.test.MyEntity;
import io.quarkus.hibernate.panache.deployment.test.MyReactiveEntity;

public class ReactivePanacheWithoutHibernateReactiveTest {

    @Test
    public void reactiveEntityWithoutHibernateReactiveIsRejected() {
        Capabilities capabilities = new Capabilities(Collections.emptySet());
        Set<String> offendingTypes = ReactivePanacheValidator.findOffendingReactivePanacheTypes(
                ReactivePanacheValidator.indexOf(MyReactiveEntity.class), capabilities);

        assertThat(offendingTypes).contains(MyReactiveEntity.class.getName());
    }

    @Test
    public void blockingEntityWithoutHibernateReactiveIsAllowed() {
        Capabilities capabilities = new Capabilities(Collections.emptySet());
        Set<String> offendingTypes = ReactivePanacheValidator.findOffendingReactivePanacheTypes(
                ReactivePanacheValidator.indexOf(MyEntity.class), capabilities);

        assertThat(offendingTypes).isEmpty();
    }
}
