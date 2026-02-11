/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.hibernate.panache.deployment.test.processor;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EntityWithBadRepoNamesTest {

    @Test
    public void test() throws Exception {
        // Panache entity
        Class<?> entityClass = EntityWithBadRepoNames_.class;
        Assertions.assertNotNull(entityClass);

        // Make sure the accessor types are correct
        Method accessor = entityClass.getDeclaredMethod("managedBlocking");
        Assertions.assertEquals(EntityWithBadRepoNames.ManagedBlocking.class, accessor.getReturnType());

        accessor = entityClass.getDeclaredMethod("statelessBlocking");
        Assertions.assertEquals(EntityWithBadRepoNames.StatelessBlocking.class, accessor.getReturnType());

        accessor = entityClass.getDeclaredMethod("managedReactive");
        Assertions.assertEquals(EntityWithBadRepoNames.ManagedReactive.class, accessor.getReturnType());

        accessor = entityClass.getDeclaredMethod("statelessReactive");
        Assertions.assertEquals(EntityWithBadRepoNames.StatelessReactive.class, accessor.getReturnType());
    }
}
