package io.quarkus.hibernate.orm.applicationfieldaccess;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that access to public fields by the application is correctly replaced with getter/setter calls
 * and works correctly for all field types.
 */
public class PublicFieldAccessFieldTypesTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
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
        // Ideally we'd write a @ParameterizedTest and pass the delegates as parameters,
        // but we cannot do that due to JUnit using a different classloader than the test.
        for (FieldAccessEnhancedDelegate delegate : FieldAccessEnhancedDelegate.values()) {
            doTestFieldAccess(delegate);
        }
    }

    private void doTestFieldAccess(FieldAccessEnhancedDelegate delegate)
            throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        MyEntity entity = new MyEntity();

        transaction.begin();
        em.persist(entity);
        transaction.commit();

        transaction.begin();
        entity = em.getReference(MyEntity.class, entity.id);
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
        entity = em.getReference(MyEntity.class, entity.id);
        // Since field access is replaced with accessor calls,
        // we expect this change to be detected by dirty tracking and persisted.
        delegate.setValue(entity);
        transaction.commit();

        transaction.begin();
        entity = em.getReference(MyEntity.class, entity.id);
        // We're working on an uninitialized proxy.
        assertThat(entity).returns(false, Hibernate::isInitialized);
        // The above should have persisted a value that passes the assertion.
        delegate.assertValue(entity);
        // Accessing the value should trigger initialization of the proxy.
        assertThat(entity).returns(true, Hibernate::isInitialized);
        transaction.rollback();
    }

    @Entity
    public static class MyEntity {

        @Id
        @GeneratedValue
        public long id;

        public LocalDate object;
        public boolean boolean_;
        public int int_;
        public long long_;
        public float float_;
        public double double_;
        public short short_;
        public char char_;
        public byte byte_;

    }

    private enum FieldAccessEnhancedDelegate {

        OBJECT {
            @Override
            public void setValue(MyEntity entity) {
                entity.object = LocalDate.of(2017, 11, 7);
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.object).isEqualTo(LocalDate.of(2017, 11, 7));
            }
        },
        BOOLEAN {
            @Override
            public void setValue(MyEntity entity) {
                entity.boolean_ = true;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.boolean_).isTrue();
            }
        },
        INTEGER {
            @Override
            public void setValue(MyEntity entity) {
                entity.int_ = 42;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.int_).isEqualTo(42);
            }
        },
        LONG {
            @Override
            public void setValue(MyEntity entity) {
                entity.long_ = 42L;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.long_).isEqualTo(42L);
            }
        },
        FLOAT {
            @Override
            public void setValue(MyEntity entity) {
                entity.float_ = 42f;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.float_).isEqualTo(42f);
            }
        },
        DOUBLE {
            @Override
            public void setValue(MyEntity entity) {
                entity.double_ = 42d;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.double_).isEqualTo(42d);
            }
        },
        SHORT {
            @Override
            public void setValue(MyEntity entity) {
                entity.short_ = (short) 42;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.short_).isEqualTo((short) 42);
            }
        },
        CHAR {
            @Override
            public void setValue(MyEntity entity) {
                entity.char_ = 'a';
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.char_).isEqualTo('a');
            }
        },
        BYTE {
            @Override
            public void setValue(MyEntity entity) {
                entity.byte_ = (byte) 42;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.byte_).isEqualTo((byte) 42);
            }
        };

        public abstract void setValue(MyEntity entity);

        public abstract void assertValue(MyEntity entity);

    }
}
