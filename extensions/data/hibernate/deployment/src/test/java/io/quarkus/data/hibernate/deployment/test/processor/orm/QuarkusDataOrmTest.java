/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.hibernate.deployment.test.processor.orm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import jakarta.inject.Inject;
import jakarta.persistence.EntityAgent;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.data.hibernate.WithId;

public class QuarkusDataOrmTest {
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

        // Nested repo accessor
        method = entityClass.getDeclaredMethod("JDQueries");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(QuarkusDataBook.JDQueries.class, method.getReturnType());

        // Nested repo accessor
        method = entityClass.getDeclaredMethod("myRepo");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(QuarkusDataBook.MyRepo.class, method.getReturnType());

        // Predefined repo accessors
        method = entityClass.getDeclaredMethod("managed");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(QuarkusDataBook.class.getName() + "$MyRepo", method.getReturnType().getName());

        method = entityClass.getDeclaredMethod("record");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(QuarkusDataBook.class.getName() + "$StatelessRepo",
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
        method = entityClass.getDeclaredMethod("managed");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(QuarkusDataBookCustomId.ManagedQueries.class, method.getReturnType());

        method = entityClass.getDeclaredMethod("record");
        Assertions.assertNotNull(method);
        Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
        Assertions.assertEquals(QuarkusDataBookCustomId.StatelessQueries.class, method.getReturnType());

        Class<?> managedQueriesClass = _QuarkusDataBookCustomId._ManagedQueries.class;
        Assertions.assertNotNull(managedQueriesClass);
        // make sure it's a repository
        Assertions.assertFalse(Modifier.isAbstract(managedQueriesClass.getModifiers()));
        Class<?>[] interfaces = managedQueriesClass.getInterfaces();
        Assertions.assertEquals(1, interfaces.length);
        Assertions.assertEquals(QuarkusDataBookCustomId.ManagedQueries.class.getName(), interfaces[0].getName());

        Constructor<?> constructor = managedQueriesClass.getConstructor(Session.class);
        Assertions.assertNotNull(constructor);

        Class<?> statelessQueriesClass = _QuarkusDataBookCustomId._StatelessQueries.class;
        Assertions.assertNotNull(statelessQueriesClass);
        // make sure it's a repository
        Assertions.assertFalse(Modifier.isAbstract(statelessQueriesClass.getModifiers()));
        interfaces = statelessQueriesClass.getInterfaces();
        Assertions.assertEquals(1, interfaces.length);
        Assertions.assertEquals(QuarkusDataBookCustomId.StatelessQueries.class.getName(), interfaces[0].getName());

        constructor = statelessQueriesClass.getConstructor(StatelessSession.class);
        Assertions.assertNotNull(constructor);
    }

    @Test
    public void testPlainInterfaceRepository() throws Exception {
        Class<?> repositoryClass = _PlainBookRepository.class;
        Assertions.assertFalse(Modifier.isAbstract(repositoryClass.getModifiers()));

        Class<?>[] interfaces = repositoryClass.getInterfaces();
        Assertions.assertEquals(1, interfaces.length);
        Assertions.assertEquals(PlainBookRepository.class.getName(), interfaces[0].getName());

        // Annotated methods generate instance methods
        Method method = repositoryClass.getDeclaredMethod("hqlBook", String.class);
        Assertions.assertFalse(Modifier.isStatic(method.getModifiers()));
        method = repositoryClass.getDeclaredMethod("findBook", String.class);
        Assertions.assertFalse(Modifier.isStatic(method.getModifiers()));

        // The default blocking session is injected
        Constructor<?> constructor = repositoryClass.getDeclaredConstructor(Session.class);
        Assertions.assertTrue(constructor.isAnnotationPresent(Inject.class));
    }

    @Test
    public void testJakartaDataRepository() throws Exception {
        Class<?> repositoryClass = _BookJakartaDataRepository.class;
        Assertions.assertFalse(Modifier.isAbstract(repositoryClass.getModifiers()));

        Class<?>[] interfaces = repositoryClass.getInterfaces();
        Assertions.assertEquals(1, interfaces.length);
        Assertions.assertEquals(BookJakartaDataRepository.class.getName(), interfaces[0].getName());

        // Annotated methods generate instance methods
        Method method = repositoryClass.getDeclaredMethod("hqlBook", String.class);
        Assertions.assertFalse(Modifier.isStatic(method.getModifiers()));
        method = repositoryClass.getDeclaredMethod("findBook", String.class);
        Assertions.assertFalse(Modifier.isStatic(method.getModifiers()));

        // Jakarta Data repositories get an EntityAgent injected
        Constructor<?> constructor = repositoryClass.getDeclaredConstructor(EntityAgent.class);
        Assertions.assertTrue(constructor.isAnnotationPresent(Inject.class));
    }

    @Test
    public void testInheritedEntityDoesNotRedeclareRepositoryAccessors() throws Exception {
        // The parent declares the default repository accessors...
        Class<?> parentClass = InheritedParentEntity_.class;
        Assertions.assertNotNull(parentClass.getDeclaredMethod("managed"));
        Assertions.assertNotNull(parentClass.getDeclaredMethod("record"));

        // ...and the child metamodel extends the parent one, so it must not redeclare them with
        // an entity-specific return type, which would be an invalid static method hiding
        Class<?> childClass = InheritedChildEntity_.class;
        Assertions.assertEquals(parentClass, childClass.getSuperclass());
        Assertions.assertThrows(NoSuchMethodException.class, () -> childClass.getDeclaredMethod("managed"));
        Assertions.assertThrows(NoSuchMethodException.class, () -> childClass.getDeclaredMethod("record"));
    }
}
