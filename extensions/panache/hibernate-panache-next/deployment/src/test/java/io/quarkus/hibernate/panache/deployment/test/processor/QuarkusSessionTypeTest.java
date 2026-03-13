/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.hibernate.panache.deployment.test.processor;

import java.lang.reflect.Constructor;

import jakarta.inject.Inject;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class QuarkusSessionTypeTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(SessionTypeEntity.class, SessionTypeEntity_.class,
                            SessionTypeEntity_.ManagedReactiveRepo_.class,
                            SessionTypeEntity_.StatelessReactiveRepo_.class));

    @RunOnVertxContext
    @Test
    public void test(UniAsserter asserter) throws Exception {
        // Panache entity
        Class<?> entityClass = SessionTypeEntity_.class;
        Assertions.assertNotNull(entityClass);

        // Make sure the repositories have the proper sessions
        Class<?> repoClass = getDeclaredClass(entityClass, "ManagedBlockingRepo_");
        Constructor<?> constructor = repoClass.getDeclaredConstructor(Session.class);
        Assertions.assertNotNull(constructor);
        Assertions.assertTrue(constructor.isAnnotationPresent(Inject.class));

        repoClass = getDeclaredClass(entityClass, "StatelessBlockingRepo_");
        constructor = repoClass.getDeclaredConstructor(StatelessSession.class);
        Assertions.assertNotNull(constructor);
        Assertions.assertTrue(constructor.isAnnotationPresent(Inject.class));

        // I don't know how to test that they got the proper session without invoking it
        repoClass = getDeclaredClass(entityClass, "ManagedReactiveRepo_");
        Assertions.assertEquals(1, repoClass.getDeclaredConstructors().length);
        Assertions.assertEquals(0, repoClass.getDeclaredConstructors()[0].getParameterCount());

        repoClass = getDeclaredClass(entityClass, "StatelessReactiveRepo_");
        Assertions.assertEquals(1, repoClass.getDeclaredConstructors().length);
        Assertions.assertEquals(0, repoClass.getDeclaredConstructors()[0].getParameterCount());

        asserter.execute(() -> insertOne());
        asserter.execute(() -> checkManaged());
        asserter.execute(() -> checkStateless());
    }

    @WithTransaction
    Uni<Void> insertOne() {
        return SessionTypeEntity_.managedReactiveRepo().persist(new SessionTypeEntity());
    }

    @WithTransaction
    Uni<Void> checkManaged() {
        return SessionTypeEntity_.managedReactiveRepo().all().flatMap(all -> {
            Assertions.assertEquals(1, all.size());
            return SessionTypeEntity_.managedReactiveRepo().isPersistent(all.get(0));
        }).map(isPersistent -> {
            Assertions.assertTrue(isPersistent);
            return null;
        });
    }

    Uni<Void> checkStateless() {
        // I cannot find how to figure out if an entity is stateless (since there's not session association), so I
        // ressort to modifying it in a TX and checking that the changes have not been sent to the DB in another TX
        return checkStatelessModify().chain(() -> checkStatelessNotModified());
    }

    @WithTransaction(stateless = true)
    Uni<Void> checkStatelessModify() {
        return SessionTypeEntity_.statelessReactiveRepo().all().map(all -> {
            Assertions.assertEquals(1, all.size());
            all.get(0).field = "something";
            return null;
        });
    }

    @WithTransaction(stateless = true)
    Uni<Void> checkStatelessNotModified() {
        return SessionTypeEntity_.statelessReactiveRepo().all().map(all -> {
            Assertions.assertEquals(1, all.size());
            Assertions.assertNull(all.get(0).field);
            return null;
        });
    }

    private Class<?> getDeclaredClass(Class<?> entityClass, String name) {
        for (Class<?> declaredClass : entityClass.getDeclaredClasses()) {
            if (declaredClass.getSimpleName().equals(name)) {
                return declaredClass;
            }
        }
        Assertions.fail("Cound not find member class " + name + " in " + entityClass);
        return null;
    }
}
