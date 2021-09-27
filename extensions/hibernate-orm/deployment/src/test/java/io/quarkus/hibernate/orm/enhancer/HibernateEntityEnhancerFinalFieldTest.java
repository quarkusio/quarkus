/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package io.quarkus.hibernate.orm.enhancer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.hibernate.annotations.Immutable;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that final field write access is not replaced with a call to an internal Hibernate ORM method
 * (since writes to final fields can only occur from constructors).
 * <p>
 * See https://github.com/quarkusio/quarkus/issues/20186
 */
public class HibernateEntityEnhancerFinalFieldTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(
                            EntityWithFinalField.class,
                            EntityWithEmbeddedIdWithFinalField.class, EntityWithEmbeddedIdWithFinalField.EmbeddableId.class,
                            EntityWithEmbeddedNonIdWithFinalField.class,
                            EntityWithEmbeddedNonIdWithFinalField.EmbeddableNonId.class))
            .withConfigurationResource("application.properties");

    @Inject
    EntityManager em;

    @Inject
    UserTransaction transaction;

    @Test
    public void entityWithFinalField_constructor() {
        EntityWithFinalField entity = new EntityWithFinalField("foo");
        assertThat(entity.immutableProperty).isEqualTo("foo");
    }

    // Just test that the embedded non-ID works correctly over a persist/retrieve cycle
    @Test
    public void entityWithFinalField_smokeTest() {
        EntityWithFinalField persistedEntity = new EntityWithFinalField("foo");
        persistedEntity.setName("Some name");
        inTransaction(() -> {
            em.persist(persistedEntity);
        });

        inTransaction(() -> {
            EntityWithFinalField entity = em.find(EntityWithFinalField.class, persistedEntity.getId());
            assertThat(entity).extracting(EntityWithFinalField::getImmutableProperty)
                    .isEqualTo(persistedEntity.getImmutableProperty());
        });
    }

    // Just test that the embedded ID works correctly over a persist/retrieve cycle
    @Test
    public void embeddableIdWithFinalField_smokeTest() {
        EntityWithEmbeddedIdWithFinalField persistedEntity = new EntityWithEmbeddedIdWithFinalField();
        persistedEntity.setName("Some name");
        inTransaction(() -> {
            em.persist(persistedEntity);
        });

        // Read with the same ID instance
        inTransaction(() -> {
            EntityWithEmbeddedIdWithFinalField entity = em.find(EntityWithEmbeddedIdWithFinalField.class,
                    persistedEntity.getId());
            assertThat(entity).extracting(EntityWithEmbeddedIdWithFinalField::getId).extracting(i -> i.id)
                    .isEqualTo(persistedEntity.getId().id);
        });

        // Read with a new ID instance
        inTransaction(() -> {
            EntityWithEmbeddedIdWithFinalField entity = em.find(EntityWithEmbeddedIdWithFinalField.class,
                    EntityWithEmbeddedIdWithFinalField.EmbeddableId.of(persistedEntity.getId().id));
            assertThat(entity).extracting(EntityWithEmbeddedIdWithFinalField::getId).extracting(i -> i.id)
                    .isEqualTo(persistedEntity.getId().id);
        });

        // Read with a query
        // This is special because in this particular test,
        // we know Hibernate ORM *has to* instantiate the EmbeddableIdType itself:
        // it cannot reuse the ID we passed.
        // And since the EmbeddableIdType has a final field, instantiation will not be able to use a no-arg constructor...
        inTransaction(() -> {
            EntityWithEmbeddedIdWithFinalField entity = em
                    .createQuery("from embidwithfinal e where e.name = :name", EntityWithEmbeddedIdWithFinalField.class)
                    .setParameter("name", persistedEntity.getName())
                    .getSingleResult();
            assertThat(entity).extracting(EntityWithEmbeddedIdWithFinalField::getId).extracting(i -> i.id)
                    .isEqualTo(persistedEntity.getId().id);
        });
    }

    @Test
    public void embeddableNonIdWithFinalField_constructor() {
        EntityWithEmbeddedNonIdWithFinalField.EmbeddableNonId embeddable = new EntityWithEmbeddedNonIdWithFinalField.EmbeddableNonId(
                "foo");
        assertThat(embeddable.immutableProperty).isEqualTo("foo");
    }

    // Just test that the embedded non-ID works correctly over a persist/retrieve cycle
    @Test
    public void embeddableNonIdWithFinalField_smokeTest() {
        EntityWithEmbeddedNonIdWithFinalField persistedEntity = new EntityWithEmbeddedNonIdWithFinalField();
        persistedEntity.setName("Some name");
        persistedEntity.setEmbedded(new EntityWithEmbeddedNonIdWithFinalField.EmbeddableNonId("foo"));
        inTransaction(() -> {
            em.persist(persistedEntity);
        });

        inTransaction(() -> {
            EntityWithEmbeddedNonIdWithFinalField entity = em.find(EntityWithEmbeddedNonIdWithFinalField.class,
                    persistedEntity.getId());
            assertThat(entity).extracting(EntityWithEmbeddedNonIdWithFinalField::getEmbedded)
                    .extracting(EntityWithEmbeddedNonIdWithFinalField.EmbeddableNonId::getImmutableProperty)
                    .isEqualTo(persistedEntity.getEmbedded().getImmutableProperty());
        });
    }

    private void inTransaction(Runnable runnable) {
        try {
            transaction.begin();
            try {
                runnable.run();
                transaction.commit();
            } catch (Exception e) {
                transaction.rollback();
            }
        } catch (SystemException | NotSupportedException e) {
            throw new IllegalStateException("Transaction exception", e);
        }
    }

    @Entity(name = "withfinal")
    public static class EntityWithFinalField {

        @Id
        @GeneratedValue
        private Long id;

        private final String immutableProperty;

        private String name;

        // For Hibernate ORM
        protected EntityWithFinalField() {
            this.immutableProperty = null;
        }

        private EntityWithFinalField(String id) {
            this.immutableProperty = id;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getImmutableProperty() {
            return immutableProperty;
        }
    }

    @Entity(name = "embidwithfinal")
    public static class EntityWithEmbeddedIdWithFinalField {

        @EmbeddedId
        private EmbeddableId id;

        private String name;

        public EntityWithEmbeddedIdWithFinalField() {
            this.id = EmbeddableId.of(UUID.randomUUID().toString());
        }

        public EmbeddableId getId() {
            return id;
        }

        public void setId(EmbeddableId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Immutable
        @Embeddable
        public static class EmbeddableId implements Serializable {
            private final String id;

            // For Hibernate ORM
            protected EmbeddableId() {
                this.id = null;
            }

            private EmbeddableId(String id) {
                this.id = id;
            }

            public static EmbeddableId of(String string) {
                return new EmbeddableId(string);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (!(o instanceof EmbeddableId)) {
                    return false;
                }
                EmbeddableId embeddableIdType = (EmbeddableId) o;
                return Objects.equals(id, embeddableIdType.id);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id);
            }
        }
    }

    @Entity(name = "embwithfinal")
    public static class EntityWithEmbeddedNonIdWithFinalField {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

        @Embedded
        private EmbeddableNonId embedded;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public EmbeddableNonId getEmbedded() {
            return embedded;
        }

        public void setEmbedded(
                EmbeddableNonId embedded) {
            this.embedded = embedded;
        }

        @Embeddable
        public static class EmbeddableNonId {
            private final String immutableProperty;

            private String mutableProperty;

            protected EmbeddableNonId() {
                // For Hibernate ORM only - it will change the property value through reflection
                this.immutableProperty = null;
            }

            private EmbeddableNonId(String immutableProperty) {
                this.immutableProperty = immutableProperty;
            }

            public String getImmutableProperty() {
                return immutableProperty;
            }

            public String getMutableProperty() {
                return mutableProperty;
            }

            public void setMutableProperty(String mutableProperty) {
                this.mutableProperty = mutableProperty;
            }
        }
    }
}
