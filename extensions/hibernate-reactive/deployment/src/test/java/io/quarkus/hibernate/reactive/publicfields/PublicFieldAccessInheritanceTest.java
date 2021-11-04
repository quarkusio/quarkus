package io.quarkus.hibernate.reactive.publicfields;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that public field access is correctly replaced with getter/setter calls,
 * regardless of where the field is declared in the class hierarchy.
 */
public class PublicFieldAccessInheritanceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyMappedSuperclass.class)
                    .addClass(MyAbstractEntity.class)
                    .addClass(MyConcreteEntity.class)
                    .addClass(FieldAccessEnhancedDelegate.class))
            .withConfigurationResource("application.properties");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    public void testFieldAccess()
            throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        // Ideally we'd write a @ParamaterizedTest and pass the delegates as parameters,
        // but we cannot do that due to JUnit using a different classloader than the test.
        for (FieldAccessEnhancedDelegate delegate : FieldAccessEnhancedDelegate
                .values()) {
            doTestFieldAccess(delegate);
        }
    }

    private void doTestFieldAccess(FieldAccessEnhancedDelegate delegate) {
        long id = sessionFactory.withTransaction((session, tx) -> {
            MyConcreteEntity entity = new MyConcreteEntity();
            return session.persist(entity).replaceWith(() -> entity.id);
        }).await().indefinitely();

        MyConcreteEntity entity = sessionFactory.withTransaction((session, tx) -> session.find(MyConcreteEntity.class, id))
                .await().indefinitely();
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

        sessionFactory.withTransaction((session, tx) -> session.find(MyConcreteEntity.class, id)
                // Since field access is replaced with accessor calls,
                // we expect this change to be detected by dirty tracking and persisted.
                .invoke(delegate::setValue))
                .await().indefinitely();

        entity = sessionFactory.withTransaction((session, tx) -> session.find(MyConcreteEntity.class, id))
                .await().indefinitely();
        // The above should have persisted a value that passes the assertion.
        delegate.assertValue(entity);
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
