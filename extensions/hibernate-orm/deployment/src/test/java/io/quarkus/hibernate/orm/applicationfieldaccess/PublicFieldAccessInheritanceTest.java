package io.quarkus.hibernate.orm.applicationfieldaccess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.LogManager;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.hibernate.Hibernate;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that access to public fields by the application is correctly replaced with getter/setter calls
 * and works correctly regardless of where the field is declared in the class hierarchy.
 */
public class PublicFieldAccessInheritanceTest {

    // FIXME Temporary debug options for https://github.com/quarkusio/quarkus/issues/42479
    // Needs to be set very early (e.g. as system properties) in order to affect the build;
    // see https://quarkusio.zulipchat.com/#narrow/stream/187038-dev/topic/Build.20logs
    private static final Map<String, String> DEBUG_PROPERTIES = Map.of(
            "quarkus.debug.transformed-classes-dir", "target/debug/${testRunId}/transformed-classes",
            "quarkus.debug.generated-classes-dir", "target/debug/${testRunId}/generated-classes");
    // FIXME Temporary trace categories for https://github.com/quarkusio/quarkus/issues/42479
    // Needs to be set very early (e.g. programmatically) in order to affect the build;
    // needs to be set programmatically in order to not leak to other tests.
    // See https://quarkusio.zulipchat.com/#narrow/stream/187038-dev/topic/Build.20logs
    // See https://github.com/quarkusio/quarkus/issues/43180
    private static final List<String> TRACE_CATEGORIES = List.of("org.hibernate", "io.quarkus.hibernate", "io.quarkus.panache");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyMappedSuperclass.class)
                    .addClass(MyAbstractEntity.class)
                    .addClass(MyConcreteEntity.class)
                    .addClass(FieldAccessEnhancedDelegate.class))
            .withConfigurationResource("application.properties")
            // FIXME Temporary debug options for https://github.com/quarkusio/quarkus/issues/42479
            .overrideConfigKey("quarkus.hibernate-orm.log.sql", "true")
            // Not doing this because it has side effects on other tests for some reason;
            // see https://github.com/quarkusio/quarkus/issues/43180
            // It's not necessary anyway as the only effect of this config property is to change
            // the logging level for a specific "org.hibernate.something" category, which we already do below.
            //.overrideConfigKey("quarkus.hibernate-orm.log.bind-parameters", "true")
            .setBeforeAllCustomizer(() -> {
                // Used to differentiate reruns of flaky tests in Maven
                var testRunId = PublicFieldAccessInheritanceTest.class + "/" + UUID.randomUUID();
                System.out.println("Test run ID: " + testRunId);
                DEBUG_PROPERTIES.forEach((key, value) -> System.setProperty(key, value.replace("${testRunId}", testRunId)));
                for (String category : TRACE_CATEGORIES) {
                    LogManager.getLogManager().getLogger(category)
                            .setLevel(Level.TRACE);
                }
            })
            .setAfterAllCustomizer(() -> {
                DEBUG_PROPERTIES.keySet().forEach(System::clearProperty);
                for (String category : TRACE_CATEGORIES) {
                    LogManager.getLogManager().getLogger(category)
                            .setLevel(null);
                }
            });

    @Inject
    EntityManager em;

    @Inject
    UserTransaction transaction;

    @Test
    public void testFieldAccess()
            throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        // Ideally we'd write a @ParameterizedTest and pass the delegates as parameters,
        // but we cannot do that due to JUnit using a different classloader than the test.
        for (FieldAccessEnhancedDelegate delegate : FieldAccessEnhancedDelegate.values()) {
            doTestFieldAccess(delegate);
        }
    }

    private void doTestFieldAccess(FieldAccessEnhancedDelegate delegate)
            throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        MyConcreteEntity entity = new MyConcreteEntity();

        transaction.begin();
        em.persist(entity);
        transaction.commit();

        transaction.begin();
        entity = em.getReference(MyConcreteEntity.class, entity.id);
        // Initially the assertion doesn't pass: the value was not set yet
        AssertionError expected = null;
        try {
            delegate.assertValue(entity);
        } catch (AssertionError e) {
            expected = e;
        }
        if (expected == null) {
            throw new IllegalStateException("This test is buggy: assertions should not pass at this point.");
        }
        transaction.rollback();

        transaction.begin();
        entity = em.getReference(MyConcreteEntity.class, entity.id);
        // Since field access is replaced with accessor calls,
        // we expect this change to be detected by dirty tracking and persisted.
        delegate.setValue(entity);
        transaction.commit();

        transaction.begin();
        entity = em.getReference(MyConcreteEntity.class, entity.id);
        // We're working on an uninitialized proxy.
        assertThat(entity).returns(false, Hibernate::isInitialized);
        // The above should have persisted a value that passes the assertion.
        delegate.assertValue(entity);
        // Accessing the value should trigger initialization of the proxy.
        assertThat(entity).returns(true, Hibernate::isInitialized);
        transaction.rollback();
    }

    @MappedSuperclass
    public static class MyMappedSuperclass {

        public Long mappedSuperclassField;

    }

    @Entity(name = "abstract")
    public static abstract class MyAbstractEntity extends MyMappedSuperclass {

        @Id
        @GeneratedValue
        public long id;

        public Long abstractEntityField;

    }

    @Entity(name = "concrete")
    public static class MyConcreteEntity extends MyAbstractEntity {

        public Long concreteEntityField;

    }

    private enum FieldAccessEnhancedDelegate {

        MAPPED_SUPERCLASS {
            @Override
            public void setValue(MyConcreteEntity entity) {
                entity.mappedSuperclassField = 42L;
            }

            @Override
            public void assertValue(MyConcreteEntity entity) {
                assertThat(entity.mappedSuperclassField).isEqualTo(42L);
            }
        },
        ABSTRACT_ENTITY {
            @Override
            public void setValue(MyConcreteEntity entity) {
                entity.abstractEntityField = 42L;
            }

            @Override
            public void assertValue(MyConcreteEntity entity) {
                assertThat(entity.abstractEntityField).isEqualTo(42L);
            }
        },
        CONCRETE_ENTITY {
            @Override
            public void setValue(MyConcreteEntity entity) {
                entity.concreteEntityField = 42L;
            }

            @Override
            public void assertValue(MyConcreteEntity entity) {
                assertThat(entity.concreteEntityField).isEqualTo(42L);
            }
        };

        public abstract void setValue(MyConcreteEntity entity);

        public abstract void assertValue(MyConcreteEntity entity);

    }
}
