package io.quarkus.hibernate.orm.applicationfieldaccess;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that access to fields or getters of embeddable records by the application works correctly.
 * See https://github.com/quarkusio/quarkus/issues/36747
 */
public class RecordFieldAccessTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addClasses(MyRecordEmbeddableWithoutAdditionalGetters.class)
                    .addClasses(MyRecordEmbeddableWithAdditionalGetters.class)
                    .addClass(AccessDelegate.class))
            .withConfigurationResource("application.properties");

    @Inject
    EntityManager em;

    @Test
    public void recordWithoutAdditionalGetters_field() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyEntity entity, Long value) {
                entity.embeddedWithoutAdditionalGetters = new MyRecordEmbeddableWithoutAdditionalGetters(value);
            }

            @Override
            public Long getValue(MyEntity entity) {
                return entity.embeddedWithoutAdditionalGetters == null ? null : entity.embeddedWithoutAdditionalGetters.value;
            }
        });
    }

    @Test
    public void recordWithoutAdditionalGetters_recordGetter() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyEntity entity, Long value) {
                entity.embeddedWithoutAdditionalGetters = new MyRecordEmbeddableWithoutAdditionalGetters(value);
            }

            @Override
            public Long getValue(MyEntity entity) {
                return entity.embeddedWithoutAdditionalGetters == null ? null : entity.embeddedWithoutAdditionalGetters.value();
            }
        });
    }

    @Test
    public void recordWithAdditionalGetters_field() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyEntity entity, Long value) {
                entity.embeddedWithAdditionalGetters = new MyRecordEmbeddableWithAdditionalGetters(value);
            }

            @Override
            public Long getValue(MyEntity entity) {
                return entity.embeddedWithAdditionalGetters == null ? null : entity.embeddedWithAdditionalGetters.value;
            }
        });
    }

    @Test
    public void recordWithAdditionalGetters_recordGetter() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyEntity entity, Long value) {
                entity.embeddedWithAdditionalGetters = new MyRecordEmbeddableWithAdditionalGetters(value);
            }

            @Override
            public Long getValue(MyEntity entity) {
                return entity.embeddedWithAdditionalGetters == null ? null : entity.embeddedWithAdditionalGetters.value();
            }
        });
    }

    @Test
    public void recordWithAdditionalGetters_additionalGetter() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyEntity entity, Long value) {
                entity.embeddedWithAdditionalGetters = new MyRecordEmbeddableWithAdditionalGetters(value);
            }

            @Override
            public Long getValue(MyEntity entity) {
                return entity.embeddedWithAdditionalGetters == null ? null : entity.embeddedWithAdditionalGetters.getValue();
            }
        });
    }

    // Ideally we'd make this a @ParameterizedTest and pass the access delegate as parameter,
    // but we cannot do that due to JUnit using a different classloader than the test.
    private void doTestFieldAccess(AccessDelegate delegate) {
        Long id = QuarkusTransaction.disallowingExisting().call(() -> {
            var entity = new MyEntity();
            em.persist(entity);
            return entity.id;
        });

        QuarkusTransaction.disallowingExisting().run(() -> {
            var entity = em.find(MyEntity.class, id);
            assertThat(delegate.getValue(entity))
                    .as("Loaded value before update")
                    .isNull();
        });

        QuarkusTransaction.disallowingExisting().run(() -> {
            var entity = em.getReference(MyEntity.class, id);
            // Since field access is replaced with accessor calls,
            // we expect this change to be detected by dirty tracking and persisted.
            delegate.setValue(entity, 42L);
        });

        QuarkusTransaction.disallowingExisting().run(() -> {
            var entity = em.find(MyEntity.class, id);
            // We're working on an initialized entity.
            assertThat(entity)
                    .as("find() should return uninitialized entity")
                    .returns(true, Hibernate::isInitialized);
            // The above should have persisted a value that passes the assertion.
            assertThat(delegate.getValue(entity))
                    .as("Loaded value after update")
                    .isEqualTo(42L);
        });

        QuarkusTransaction.disallowingExisting().run(() -> {
            var entity = em.getReference(MyEntity.class, id);
            // We're working on an uninitialized entity.
            assertThat(entity)
                    .as("getReference() should return uninitialized entity")
                    .returns(false, Hibernate::isInitialized);
            // The above should have persisted a value that passes the assertion.
            assertThat(delegate.getValue(entity))
                    .as("Lazily loaded value after update")
                    .isEqualTo(42L);
            // Accessing the value should trigger initialization of the entity.
            assertThat(entity)
                    .as("Getting the value should initialize the entity")
                    .returns(true, Hibernate::isInitialized);
        });
    }

    @Entity(name = "myentity")
    public static class MyEntity {
        @Id
        @GeneratedValue
        public long id;
        @Embedded
        @AttributeOverride(name = "value", column = @Column(name = "value1"))
        public MyRecordEmbeddableWithAdditionalGetters embeddedWithAdditionalGetters;
        @Embedded
        @AttributeOverride(name = "value", column = @Column(name = "value2"))
        public MyRecordEmbeddableWithoutAdditionalGetters embeddedWithoutAdditionalGetters;
    }

    @Embeddable
    public record MyRecordEmbeddableWithoutAdditionalGetters(Long value) {
    }

    @Embeddable
    public record MyRecordEmbeddableWithAdditionalGetters(Long value) {
        Long getValue() {
            return value;
        }
    }

    private interface AccessDelegate {
        void setValue(MyEntity entity, Long value);

        Long getValue(MyEntity entity);
    }
}
