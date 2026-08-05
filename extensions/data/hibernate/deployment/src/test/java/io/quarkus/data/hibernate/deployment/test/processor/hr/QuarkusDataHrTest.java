/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.hibernate.deployment.test.processor.hr;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.data.hibernate.WithId;

public class QuarkusDataHrTest {
    @Test
    public void testPanacheEntityMetamodel() throws Exception {
        // Panache entity
        Class<?> entityClass = QuarkusDataBook_.class;
        Assertions.assertNotNull(entityClass);

        // Make sure it has the proper supertype
        Class<?> superclass = entityClass.getSuperclass();
        if (superclass != null) {
            Assertions.assertEquals(WithId.class.getName() + "_$AutoLong_", superclass.getName());
        }

        // Nested repo accessor
        Method method = entityClass.getDeclaredMethod("queries");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(QuarkusDataBook.Queries.class, method.getReturnType());

        // Predefined repo accessors
        method = entityClass.getDeclaredMethod("managedReactive");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(entityClass.getName() + "$PanacheManagedReactiveRepository_", method.getReturnType().getName());

        method = entityClass.getDeclaredMethod("statelessReactive");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(entityClass.getName() + "$PanacheStatelessReactiveRepository_",
                method.getReturnType().getName());
    }

    @Test
    public void testPanacheEntityCustomIdMetamodel() throws Exception {
        // Panache entity
        Class<?> entityClass = QuarkusDataBookCustomId_.class;
        Assertions.assertNotNull(entityClass);

        // Nested repo accessor
        Method method = entityClass.getDeclaredMethod("managedQueries");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(QuarkusDataBookCustomId.ManagedQueries.class, method.getReturnType());

        // Nested repo accessor
        method = entityClass.getDeclaredMethod("statelessQueries");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(QuarkusDataBookCustomId.StatelessQueries.class, method.getReturnType());

        // Predefined repo accessors
        method = entityClass.getDeclaredMethod("managedReactive");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(QuarkusDataBookCustomId.ManagedQueries.class, method.getReturnType());

        method = entityClass.getDeclaredMethod("statelessReactive");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(QuarkusDataBookCustomId.StatelessQueries.class, method.getReturnType());

        Class<?> managedQueriesClass = QuarkusDataBookCustomId_.ManagedQueries_.class;
        Assertions.assertNotNull(managedQueriesClass);
        // make sure it's a repository
        Assertions.assertFalse(Modifier.isAbstract(managedQueriesClass.getModifiers()));
        Class<?>[] interfaces = managedQueriesClass.getInterfaces();
        Assertions.assertEquals(1, interfaces.length);
        Assertions.assertEquals(QuarkusDataBookCustomId.ManagedQueries.class.getName(), interfaces[0].getName());

        Constructor<?> constructor = managedQueriesClass.getConstructor();
        Assertions.assertNotNull(constructor);

        Class<?> statelessQueriesClass = QuarkusDataBookCustomId_.StatelessQueries_.class;
        Assertions.assertNotNull(statelessQueriesClass);
        // make sure it's a repository
        Assertions.assertFalse(Modifier.isAbstract(statelessQueriesClass.getModifiers()));
        interfaces = statelessQueriesClass.getInterfaces();
        Assertions.assertEquals(1, interfaces.length);
        Assertions.assertEquals(QuarkusDataBookCustomId.StatelessQueries.class.getName(), interfaces[0].getName());

        constructor = statelessQueriesClass.getConstructor();
        Assertions.assertNotNull(constructor);
    }
}
