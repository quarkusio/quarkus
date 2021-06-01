package io.quarkus.hibernate.orm.publicfields;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.hibernate.Hibernate;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that public field access is correctly replaced with getter/setter calls,
 * regardless of the field type.
 */
public class PublicFieldAccessAssociationsTest {

    private static final String CONTAINED_VALUE = "someValue";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(ContainingEntity.class)
                    .addClass(FieldAccessEnhancedDelegate.class))
            .withConfigurationResource("application-fetch-max-depth-zero.properties");

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
        ContainingEntity entity = new ContainingEntity();
        ContainedEntity containedEntity = new ContainedEntity();
        containedEntity.value = CONTAINED_VALUE;

        transaction.begin();
        em.persist(entity);
        em.persist(containedEntity);
        transaction.commit();

        transaction.begin();
        entity = em.getReference(ContainingEntity.class, entity.id);
        containedEntity = em.getReference(ContainedEntity.class, containedEntity.id);
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
        entity = em.getReference(ContainingEntity.class, entity.id);
        containedEntity = em.getReference(ContainedEntity.class, containedEntity.id);
        // Since field access is replaced with accessor calls,
        // we expect this change to be detected by dirty tracking and persisted.
        delegate.setValue(entity, containedEntity);
        transaction.commit();

        // Test getReference()
        transaction.begin();
        entity = em.getReference(ContainingEntity.class, entity.id);
        containedEntity = em.getReference(ContainedEntity.class, containedEntity.id);
        // We're working on an uninitialized proxy.
        assertThat(entity).returns(false, Hibernate::isInitialized);
        // The above should have persisted a value that passes the assertion.
        delegate.assertValueAndLaziness(entity, containedEntity);
        // Accessing the value should trigger initialization of the proxy.
        assertThat(entity).returns(true, Hibernate::isInitialized);
        transaction.commit();

        // Test find()
        transaction.begin();
        entity = em.find(ContainingEntity.class, entity.id);
        containedEntity = em.find(ContainedEntity.class, containedEntity.id);
        // We're working on an actual entity instance (not a proxy).
        assertThat(entity).returns(true, Hibernate::isInitialized);
        // The above should have persisted a value that passes the assertion.
        delegate.assertValueAndLaziness(entity, containedEntity);
        transaction.commit();
    }

    @Entity
    public static class ContainingEntity {

        @Id
        @GeneratedValue
        public long id;

        @OneToOne
        public ContainedEntity oneToOne;

        @ManyToOne
        public ContainedEntity manyToOne;

        @OneToMany
        @JoinTable(name = "containing_oneToMany")
        public List<ContainedEntity> oneToMany = new ArrayList<>();

        @ManyToMany
        @JoinTable(name = "containing_manyToMany")
        public List<ContainedEntity> manyToMany = new ArrayList<>();

        @OneToOne(mappedBy = "oneToOne")
        public ContainedEntity oneToOneMappedBy;

        @OneToMany(mappedBy = "manyToOne")
        public List<ContainedEntity> oneToManyMappedBy = new ArrayList<>();

        @ManyToMany(mappedBy = "manyToMany")
        public List<ContainedEntity> manyToManyMappedBy = new ArrayList<>();

    }

    @Entity
    public static class ContainedEntity {

        @Id
        @GeneratedValue
        public long id;

        public String value;

        @OneToOne
        public ContainingEntity oneToOne;

        @ManyToOne
        public ContainingEntity manyToOne;

        @ManyToMany
        @JoinTable(name = "containing_manyToMany_mappedBy")
        public List<ContainingEntity> manyToMany = new ArrayList<>();

    }

    private enum FieldAccessEnhancedDelegate {

        ONE_TO_ONE {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.oneToOne = containedEntity;
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                // No expectations regarding laziness on ToOne associations
                assertThat(entity.oneToOne).isEqualTo(containedEntity);
                consumeValue(entity.oneToOne);
            }
        },
        MANY_TO_ONE {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.manyToOne = containedEntity;
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                // No expectations regarding laziness on ToOne associations
                assertThat(entity.manyToOne).isEqualTo(containedEntity);
                consumeValue(entity.manyToOne);
            }
        },
        ONE_TO_MANY {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.oneToMany.add(containedEntity);
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                assertThat((Object) entity.oneToMany).returns(false, Hibernate::isInitialized);
                assertThat(entity.oneToMany).containsExactly(containedEntity);
                assertThat((Object) entity.oneToMany).returns(true, Hibernate::isInitialized);
            }
        },
        MANY_TO_MANY {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.manyToMany.add(containedEntity);
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                assertThat((Object) entity.manyToMany).returns(false, Hibernate::isInitialized);
                assertThat(entity.manyToMany).containsExactly(containedEntity);
                assertThat((Object) entity.manyToMany).returns(true, Hibernate::isInitialized);
            }
        },
        ONE_TO_ONE_MAPPED_BY {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.oneToOneMappedBy = containedEntity;
                containedEntity.oneToOne = entity;
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                // No expectations regarding laziness on ToOne associations
                assertThat(entity.oneToOneMappedBy).isEqualTo(containedEntity);
                consumeValue(entity.oneToOneMappedBy);
            }
        },
        ONE_TO_MANY_MAPPED_BY {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.oneToManyMappedBy.add(containedEntity);
                containedEntity.manyToOne = entity;
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                assertThat((Object) entity.oneToManyMappedBy).returns(false, Hibernate::isInitialized);
                assertThat(entity.oneToManyMappedBy).containsExactly(containedEntity);
                assertThat((Object) entity.oneToManyMappedBy).returns(true, Hibernate::isInitialized);
            }
        },
        MANY_TO_MANY_MAPPED_BY {
            @Override
            public void setValue(ContainingEntity entity, ContainedEntity containedEntity) {
                entity.manyToManyMappedBy.add(containedEntity);
                containedEntity.manyToMany.add(entity);
            }

            @Override
            public void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity) {
                assertThat((Object) entity.manyToManyMappedBy).returns(false, Hibernate::isInitialized);
                assertThat(entity.manyToManyMappedBy).containsExactly(containedEntity);
                assertThat((Object) entity.manyToManyMappedBy).returns(true, Hibernate::isInitialized);
            }
        };

        protected void consumeValue(ContainedEntity entity) {
            assertThat(entity.value).isEqualTo(CONTAINED_VALUE);
        }

        public abstract void setValue(ContainingEntity entity, ContainedEntity containedEntity);

        public abstract void assertValueAndLaziness(ContainingEntity entity, ContainedEntity containedEntity);

    }
}
