/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.hibernate.panache.deployment.test.processor.orm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.panache.WithId;

public class QuarkusOrmPanache2Test {
    @Test
    public void testPanacheEntityMetamodel() throws Exception {
        // Panache entity
        Class<?> entityClass = Panache2Book_.class;
        Assertions.assertNotNull(entityClass);

        // Make sure it has the proper supertype
        Class<?> superclass = entityClass.getSuperclass();
        if (superclass != null) {
            Assertions.assertEquals(WithId.class.getName() + "_", superclass.getName());
        }

        // Nested repo accessor
        Method method = entityClass.getDeclaredMethod("queries");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(Panache2Book.Queries.class, method.getReturnType());

        // Predefined repo accessors
        method = entityClass.getDeclaredMethod("managedBlocking");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(entityClass.getName() + "$PanacheManagedBlockingRepository_", method.getReturnType().getName());

        method = entityClass.getDeclaredMethod("statelessBlocking");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(entityClass.getName() + "$PanacheStatelessBlockingRepository_",
                method.getReturnType().getName());
    }

    @Test
    public void testPanacheEntityCustomIdMetamodel() throws Exception {
        // Panache entity
        Class<?> entityClass = Panache2BookCustomId_.class;
        Assertions.assertNotNull(entityClass);

        // Nested repo accessor
        Method method = entityClass.getDeclaredMethod("managedQueries");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(Panache2BookCustomId.ManagedQueries.class, method.getReturnType());

        // Nested repo accessor
        method = entityClass.getDeclaredMethod("statelessQueries");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(Panache2BookCustomId.StatelessQueries.class, method.getReturnType());

        // Predefined repo accessors
        method = entityClass.getDeclaredMethod("managedBlocking");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(Panache2BookCustomId.ManagedQueries.class, method.getReturnType());

        method = entityClass.getDeclaredMethod("statelessBlocking");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(Panache2BookCustomId.StatelessQueries.class, method.getReturnType());

        Class<?> managedQueriesClass = Panache2BookCustomId_.ManagedQueries_.class;
        Assertions.assertNotNull(managedQueriesClass);
        // make sure it's a repository
        Assertions.assertFalse(Modifier.isAbstract(managedQueriesClass.getModifiers()));
        Class<?>[] interfaces = managedQueriesClass.getInterfaces();
        Assertions.assertEquals(1, interfaces.length);
        Assertions.assertEquals(Panache2BookCustomId.ManagedQueries.class.getName(), interfaces[0].getName());

        Constructor<?> constructor = managedQueriesClass.getConstructor(Session.class);
        Assertions.assertNotNull(constructor);

        Class<?> statelessQueriesClass = Panache2BookCustomId_.StatelessQueries_.class;
        Assertions.assertNotNull(statelessQueriesClass);
        // make sure it's a repository
        Assertions.assertFalse(Modifier.isAbstract(statelessQueriesClass.getModifiers()));
        interfaces = statelessQueriesClass.getInterfaces();
        Assertions.assertEquals(1, interfaces.length);
        Assertions.assertEquals(Panache2BookCustomId.StatelessQueries.class.getName(), interfaces[0].getName());

        constructor = statelessQueriesClass.getConstructor(StatelessSession.class);
        Assertions.assertNotNull(constructor);
    }
}
