/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.hibernate.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.data.hibernate.deployment.test.MyEntity;
import io.quarkus.data.hibernate.deployment.test.MyReactiveEntity;
import io.quarkus.deployment.Capabilities;

public class ReactivePanacheWithoutHibernateReactiveTest {

    @Test
    public void reactiveEntityWithoutHibernateReactiveIsRejected() {
        Capabilities capabilities = new Capabilities(Collections.emptySet());
        Set<String> offendingTypes = ReactivePanacheValidator.findOffendingReactivePanacheTypes(
                ReactivePanacheValidatorTestHelper.indexOf(MyReactiveEntity.class), capabilities);

        assertThat(offendingTypes).contains(MyReactiveEntity.class.getName());
    }

    @Test
    public void blockingEntityWithoutHibernateReactiveIsAllowed() {
        Capabilities capabilities = new Capabilities(Collections.emptySet());
        Set<String> offendingTypes = ReactivePanacheValidator.findOffendingReactivePanacheTypes(
                ReactivePanacheValidatorTestHelper.indexOf(MyEntity.class), capabilities);

        assertThat(offendingTypes).isEmpty();
    }
}
