package io.quarkus.hibernate.orm.publicfields;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.hibernate.Hibernate;
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
    EntityManager em;

    @Inject
    UserTransaction transaction;

    @Test
    public void testFieldAccess()
            throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        // Ideally we'd write a @ParamaterizedTest and pass the delegates as parameters,
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
