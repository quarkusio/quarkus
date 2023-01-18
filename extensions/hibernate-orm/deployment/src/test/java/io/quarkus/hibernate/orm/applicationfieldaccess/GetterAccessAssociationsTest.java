package io.quarkus.hibernate.orm.applicationfieldaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that access to fields through getters by the application works correctly for all association types.
 */
public class GetterAccessAssociationsTest {

    private static final String CONTAINED_VALUE = "someValue";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(ContainingEntity.class)
                    .addClass(AccessDelegate.class))
            .withConfigurationResource("application-fetch-max-depth-zero.properties");

    @Inject
    EntityManager em;

    @Inject
    UserTransaction transaction;

    @Test
    public void testGetterAccess()
            throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        // Ideally we'd write a @ParameterizedTest and pass the delegates as parameters,
        // but we cannot do that due to JUnit using a different classloader than the test.
        for (AccessDelegate delegate : AccessDelegate.values()) {
            doTestGetterAccess(delegate);
        }
    }

    private void doTestGetterAccess(AccessDelegate delegate)
            throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        ContainingEntity entity = new ContainingEntity();
        ContainedEntity containedEntity = new ContainedEntity();
        containedEntity.setValue(CONTAINED_VALUE);

        transaction.begin();
        em.persist(entity);
        em.persist(containedEntity);
        transaction.commit();

        transaction.begin();
        entity = em.getReference(ContainingEntity.class, entity.getId());
        containedEntity = em.getReference(ContainedEntity.class, containedEntity.getId());
        // Initially the assertion doesn't pass: the value was not set yet
        AssertionError expected = null;
        try {
            delegate.assertValueAndLaziness(entity, containedEntity);
        } catch (AssertionError e) {
            expected = e;
        }
        if (expected == null) {
            throw new IllegalStateException("This test is buggy: assertions should not pass at this point.");
        }
        transaction.rollback();

        transaction.begin();
        entity = em.getReference(ContainingEntity.class, entity.getId());
        containedEntity = em.getReference(ContainedEntity.class, containedEntity.getId());
        // Since field access is replaced with accessor calls,
        // we expect this change to be detected by dirty tracking and persisted.
        delegate.setValue(entity, containedEntity);
        transaction.commit();

        // Test getReference()
        transaction.begin();
        entity = em.getReference(ContainingEntity.class, entity.getId());
        containedEntity = em.getReference(ContainedEntity.class, containedEntity.getId());
        // We're working on an uninitialized proxy.
        assertThat(entity).returns(false, Hibernate::isInitialized);
        // The above should have persisted a value that passes the assertion.
        delegate.assertValueAndLaziness(entity, containedEntity);
        // Accessing the value should trigger initialization of the proxy.
        assertThat(entity).returns(true, Hibernate::isInitialized);
        transaction.commit();

        // Test find()
        transaction.begin();
        entity = em.find(ContainingEntity.class, entity.getId());
        containedEntity = em.find(ContainedEntity.class, containedEntity.getId());
        // We're working on an actual entity instance (not a proxy).
        assertThat(entity).returns(true, Hibernate::isInitialized);
        // The above should have persisted a value that passes the assertion.
        delegate.assertValueAndLaziness(entity, containedEntity);
        transaction.commit();

        // Test find() + access outside of session
        transaction.begin();
        entity = em.find(ContainingEntity.class, entity.getId());
        containedEntity = em.find(ContainedEntity.class, containedEntity.getId());
        // We're working on an actual entity instance (not a proxy).
        assertThat(entity).returns(true, Hibernate::isInitialized);
        transaction.commit();
        // Access to the property out of session should pass certain assertions
        // (which are different depending on the type of association).
        delegate.assertAccessOutOfSession(entity, containedEntity);
    }

    @Entity
    public static class ContainingEntity {

        @Id
        @GeneratedValue
        public long id;

        @OneToOne
        private ContainedEntity oneToOne;

        @ManyToOne
        private ContainedEntity manyToOne;

        @OneToMany
        @JoinTable(name = "containing_oneToMany")
        private List<ContainedEntity> oneToMany = new ArrayList<>();

        @ManyToMany
        @JoinTable(name = "containing_manyToMany")
        private List<ContainedEntity> manyToMany = new ArrayList<>();

        @OneToOne(mappedBy = "oneToOne")
        private ContainedEntity oneToOneMappedBy;

        @OneToMany(mappedBy = "manyToOne")
        private List<ContainedEntity> oneToManyMappedBy = new ArrayList<>();

        @ManyToMany(mappedBy = "manyToMany")
        private List<ContainedEntity> manyToManyMappedBy = new ArrayList<>();

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public ContainedEntity getOneToOne() {
            return oneToOne;
        }

        public void setOneToOne(ContainedEntity oneToOne) {
            this.oneToOne = oneToOne;
        }

        public ContainedEntity getManyToOne() {
            return manyToOne;
        }

        public void setManyToOne(ContainedEntity manyToOne) {
            this.manyToOne = manyToOne;
        }

        public List<ContainedEntity> getOneToMany() {
            return oneToMany;
        }

        public void setOneToMany(List<ContainedEntity> oneToMany) {
            this.oneToMany = oneToMany;
        }

        public List<ContainedEntity> getManyToMany() {
            return manyToMany;
        }

        public void setManyToMany(List<ContainedEntity> manyToMany) {
            this.manyToMany = manyToMany;
        }

        public ContainedEntity getOneToOneMappedBy() {
            return oneToOneMappedBy;
        }

        public void setOneToOneMappedBy(ContainedEntity oneToOneMappedBy) {
            this.oneToOneMappedBy = oneToOneMappedBy;
        }

        public List<ContainedEntity> getOneToManyMappedBy() {
            return oneToManyMappedBy;
        }

        public void setOneToManyMappedBy(List<ContainedEntity> oneToManyMappedBy) {
            this.oneToManyMappedBy = oneToManyMappedBy;
        }

        public List<ContainedEntity> getManyToManyMappedBy() {
            return manyToManyMappedBy;
        }

        public void setManyToManyMappedBy(List<ContainedEntity> manyToManyMappedBy) {
            this.manyToManyMappedBy = manyToManyMappedBy;
        }
    }

    @Entity
    public static class ContainedEntity {

        @Id
        @GeneratedValue
        private long id;

        @Column(name = "value_")
        private String value;

        @OneToOne
        private ContainingEntity oneToOne;

        @ManyToOne
        private ContainingEntity manyToOne;

        @ManyToMany
        @JoinTable(name = "containing_manyToMany_mappedBy")
        private List<ContainingEntity> manyToMany = new ArrayList<>();

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public ContainingEntity getOneToOne() {
            return oneToOne;
        }

        public void setOneToOne(ContainingEntity oneToOne) {
            this.oneToOne = oneToOne;
        }

        public ContainingEntity getManyToOne() {
            return manyToOne;
        }

        public void setManyToOne(ContainingEntity manyToOne) {
            this.manyToOne = manyToOne;
        }

        public List<ContainingEntity> getManyToMany() {
            return manyToMany;
        }

        public void setManyToMany(List<ContainingEntity> manyToMany) {
            this.manyToMany = manyToMany;
        }
    }

    private enum AccessDelegate {

        ONE_TO_ONE {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.setOneToOne(containedEntity);
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                // No expectations regarding laziness on ToOne associations
                assertThat(entity.getOneToOne()).isEqualTo(containedEntity);
                consumeValue(entity.getOneToOne());
            }

            @Override
            public void assertAccessOutOfSession(ContainingEntity entity, ContainedEntity containedEntity) {
                // No expectations regarding laziness on ToOne associations
                assertThat(entity.getOneToOne()).isEqualTo(containedEntity);
                consumeValue(entity.getOneToOne());
            }
        },
        MANY_TO_ONE {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.setManyToOne(containedEntity);
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                // No expectations regarding laziness on ToOne associations
                assertThat(entity.getManyToOne()).isEqualTo(containedEntity);
                consumeValue(entity.getManyToOne());
            }

            @Override
            public void assertAccessOutOfSession(ContainingEntity entity, ContainedEntity containedEntity) {
                // No expectations regarding laziness on ToOne associations
                assertThat(entity.getManyToOne()).isEqualTo(containedEntity);
                consumeValue(entity.getManyToOne());
            }
        },
        ONE_TO_MANY {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.getOneToMany().add(containedEntity);
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                assertThat((Object) entity.getOneToMany()).returns(false, Hibernate::isInitialized);
                assertThat(entity.getOneToMany()).containsExactly(containedEntity);
                assertThat((Object) entity.getOneToMany()).returns(true, Hibernate::isInitialized);
            }

            @Override
            public void assertAccessOutOfSession(ContainingEntity entity, ContainedEntity containedEntity) {
                // We expect to be able to call the getter outside of the session on an initialized entity.
                // https://github.com/quarkusio/quarkus/discussions/27657
                assertThatCode(() -> entity.getOneToMany()).doesNotThrowAnyException();
                // But of course, the collection is not initialized and accessing the content won't work.
                var collection = entity.getOneToMany();
                assertThat((Object) collection).returns(false, Hibernate::isInitialized);
                assertThatThrownBy(() -> collection.size()).isInstanceOf(LazyInitializationException.class);
            }
        },
        MANY_TO_MANY {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.getManyToMany().add(containedEntity);
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                assertThat((Object) entity.getManyToMany()).returns(false, Hibernate::isInitialized);
                assertThat(entity.getManyToMany()).containsExactly(containedEntity);
                assertThat((Object) entity.getManyToMany()).returns(true, Hibernate::isInitialized);
            }

            @Override
            public void assertAccessOutOfSession(ContainingEntity entity, ContainedEntity containedEntity) {
                // We expect to be able to call the getter outside of the session on an initialized entity.
                // https://github.com/quarkusio/quarkus/discussions/27657
                assertThatCode(() -> entity.getManyToMany()).doesNotThrowAnyException();
                // But of course, the collection is not initialized and accessing the content won't work.
                var collection = entity.getManyToMany();
                assertThat((Object) collection).returns(false, Hibernate::isInitialized);
                assertThatThrownBy(() -> collection.size()).isInstanceOf(LazyInitializationException.class);
            }
        },
        ONE_TO_ONE_MAPPED_BY {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.setOneToOneMappedBy(containedEntity);
                containedEntity.setOneToOne(entity);
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                // No expectations regarding laziness on ToOne associations
                assertThat(entity.getOneToOneMappedBy()).isEqualTo(containedEntity);
                consumeValue(entity.getOneToOneMappedBy());
            }

            @Override
            public void assertAccessOutOfSession(ContainingEntity entity, ContainedEntity containedEntity) {
                // No expectations regarding laziness on ToOne associations
                assertThat(entity.getOneToOneMappedBy()).isEqualTo(containedEntity);
                consumeValue(entity.getOneToOneMappedBy());
            }
        },
        ONE_TO_MANY_MAPPED_BY {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.getOneToManyMappedBy().add(containedEntity);
                containedEntity.setManyToOne(entity);
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                assertThat((Object) entity.getOneToManyMappedBy()).returns(false, Hibernate::isInitialized);
                assertThat(entity.getOneToManyMappedBy()).containsExactly(containedEntity);
                assertThat((Object) entity.getOneToManyMappedBy()).returns(true, Hibernate::isInitialized);
            }

            @Override
            public void assertAccessOutOfSession(ContainingEntity entity, ContainedEntity containedEntity) {
                // We expect to be able to call the getter outside of the session on an initialized entity.
                // https://github.com/quarkusio/quarkus/discussions/27657
                assertThatCode(() -> entity.getOneToManyMappedBy()).doesNotThrowAnyException();
                // But of course, the collection is not initialized and accessing the content won't work.
                var collection = entity.getOneToManyMappedBy();
                assertThat((Object) collection).returns(false, Hibernate::isInitialized);
                assertThatThrownBy(() -> collection.size()).isInstanceOf(LazyInitializationException.class);
            }
        },
        MANY_TO_MANY_MAPPED_BY {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.getManyToManyMappedBy().add(containedEntity);
                containedEntity.getManyToMany().add(entity);
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                assertThat((Object) entity.getManyToManyMappedBy()).returns(false, Hibernate::isInitialized);
                assertThat(entity.getManyToManyMappedBy()).containsExactly(containedEntity);
                assertThat((Object) entity.getManyToManyMappedBy()).returns(true, Hibernate::isInitialized);
            }

            @Override
            public void assertAccessOutOfSession(ContainingEntity entity, ContainedEntity containedEntity) {
                // We expect to be able to call the getter outside of the session on an initialized entity.
                // https://github.com/quarkusio/quarkus/discussions/27657
                assertThatCode(() -> entity.getManyToManyMappedBy()).doesNotThrowAnyException();
                // But of course, the collection is not initialized and accessing the content won't work.
                var collection = entity.getManyToManyMappedBy();
                assertThat((Object) collection).returns(false, Hibernate::isInitialized);
                assertThatThrownBy(() -> collection.size()).isInstanceOf(LazyInitializationException.class);
            }
        };

        protected void consumeValue(ContainedEntity entity) {
            assertThat(entity.getValue()).isEqualTo(CONTAINED_VALUE);
        }

        public abstract void setValue(ContainingEntity entity, ContainedEntity containedEntity);

        public abstract void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity);

        public abstract void assertAccessOutOfSession(ContainingEntity entity, ContainedEntity containedEntity);

    }
}
