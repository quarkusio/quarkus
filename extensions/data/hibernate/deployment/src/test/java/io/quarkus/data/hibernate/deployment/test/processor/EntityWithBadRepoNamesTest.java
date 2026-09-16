/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.hibernate.deployment.test.processor;

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
        Method accessor = entityClass.getDeclaredMethod("managed");
        Assertions.assertEquals(EntityWithBadRepoNames.Managed.class, accessor.getReturnType());

        accessor = entityClass.getDeclaredMethod("record");
        Assertions.assertEquals(EntityWithBadRepoNames.Record.class, accessor.getReturnType());

        accessor = entityClass.getDeclaredMethod("managedReactive");
        Assertions.assertEquals(EntityWithBadRepoNames.ManagedReactive.class, accessor.getReturnType());

        accessor = entityClass.getDeclaredMethod("recordReactive");
        Assertions.assertEquals(EntityWithBadRepoNames.RecordReactive.class, accessor.getReturnType());
    }
}
