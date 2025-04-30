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
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that access to record fields or record getters by the application works correctly.
 */
public class ImmutableEmbeddableFieldAccessTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addClasses(MyImmutableEmbeddableWithFieldAccess.class)
                    .addClasses(MyImmutableEmbeddableWithAccessors.class)
                    .addClass(AccessDelegate.class))
            .withConfigurationResource("application.properties");

    @Inject
    EntityManager em;

    @Test
    public void immutableEmbeddableWithoutAdditionalGetters_field() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyEntity entity, Long value) {
                var embedded = new MyImmutableEmbeddableWithFieldAccess();
                embedded.value = value;
                entity.embeddedWithoutAccessors = embedded;
            }

            @Override
            public Long getValue(MyEntity entity) {
                return entity.embeddedWithoutAccessors == null ? null : entity.embeddedWithoutAccessors.value;
            }
        });
    }

    @Test
    public void immutableEmbeddableWithAdditionalGetters_field() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyEntity entity, Long value) {
                var embedded = new MyImmutableEmbeddableWithAccessors();
                // Assuming this is changed only once on initialization,
                // which is the only way the @Immutable annotation would make sense.
                embedded.value = value;
                entity.embeddedWithFieldAccess = embedded;
            }

            @Override
            public Long getValue(MyEntity entity) {
                return entity.embeddedWithFieldAccess == null ? null : entity.embeddedWithFieldAccess.value;
            }
        });
    }

    @Test
    public void immutableEmbeddableWithAccessors() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyEntity entity, Long value) {
                var embedded = new MyImmutableEmbeddableWithAccessors();
                // Assuming this is changed only once on initialization,
                // which is the only way the @Immutable annotation would make sense.
                embedded.setValue(value);
                entity.embeddedWithFieldAccess = embedded;
            }

            @Override
            public Long getValue(MyEntity entity) {
                return entity.embeddedWithFieldAccess == null ? null : entity.embeddedWithFieldAccess.getValue();
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
        public MyImmutableEmbeddableWithAccessors embeddedWithFieldAccess;
        @Embedded
        @AttributeOverride(name = "value", column = @Column(name = "value2"))
        public MyImmutableEmbeddableWithFieldAccess embeddedWithoutAccessors;
    }

    @Immutable
    @Embeddable
    public static class MyImmutableEmbeddableWithFieldAccess {
        public Long value;

        public MyImmutableEmbeddableWithFieldAccess() {
        }
    }

    @Immutable
    @Embeddable
    public static class MyImmutableEmbeddableWithAccessors {
        private Long value;

        // For Hibernate ORM instantiation
        protected MyImmutableEmbeddableWithAccessors() {
        }

        public Long getValue() {
            return value;
        }

        // For Hibernate ORM instantiation
        protected void setValue(Long value) {
            this.value = value;
        }
    }

    private interface AccessDelegate {
        void setValue(MyEntity entity, Long value);

        Long getValue(MyEntity entity);
    }
}
