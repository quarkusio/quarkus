package io.quarkus.hibernate.orm.lazyloading;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.TransactionTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class LazyBasicDefaultGroupTest extends AbstractLazyBasicTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TransactionTestUtils.class)
                    .addClass(MyEntity.class)
                    .addClass(AccessDelegate.class)
                    .addClass(AccessDelegateImpl.class))
            .withConfigurationResource("application.properties");

    public LazyBasicDefaultGroupTest() {
        super(new AccessDelegateImpl());
    }

    @Entity(name = "MyEntity")
    public static class MyEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        public Long id;

        @Basic
        public String eagerProperty1;

        @Basic(fetch = FetchType.LAZY)
        public String lazyProperty1;

        @Basic(fetch = FetchType.LAZY)
        public String lazyProperty2;
    }

    private static class AccessDelegateImpl implements AccessDelegate {

        @Override
        public long create(EntityManager entityManager, String eagerProperty1, String lazyProperty1, String lazyProperty2) {
            MyEntity myEntity = new MyEntity();
            myEntity.eagerProperty1 = eagerProperty1;
            myEntity.lazyProperty1 = lazyProperty1;
            myEntity.lazyProperty2 = lazyProperty2;
            entityManager.persist(myEntity);
            return myEntity.id;
        }

        @Override
        public void updateAllProperties(EntityManager entityManager, long entityId, String eagerProperty1, String lazyProperty1,
                String lazyProperty2) {
            MyEntity entity = entityManager.find(MyEntity.class, entityId);
            entity.eagerProperty1 = eagerProperty1;
            entity.lazyProperty1 = lazyProperty1;
            entity.lazyProperty2 = lazyProperty2;
        }

        @Override
        public void updateAllLazyProperties(EntityManager entityManager, long entityId, String lazyProperty1,
                String lazyProperty2) {
            MyEntity entity = entityManager.find(MyEntity.class, entityId);
            entity.lazyProperty1 = lazyProperty1;
            entity.lazyProperty2 = lazyProperty2;
        }

        @Override
        public void updateOneEagerProperty(EntityManager entityManager, long entityId, String eagerProperty1) {
            MyEntity entity = entityManager.find(MyEntity.class, entityId);
            entity.eagerProperty1 = eagerProperty1;
        }

        @Override
        public void updateOneLazyProperty(EntityManager entityManager, long entityId, String lazyProperty1) {
            MyEntity entity = entityManager.find(MyEntity.class, entityId);
            entity.lazyProperty1 = lazyProperty1;
        }

        @Override
        public void testLazyLoadingAndPersistedValues(EntityManager entityManager, long entityId,
                String expectedEagerProperty1,
                String expectedLazyProperty1,
                String expectedLazyProperty2) {
            MyEntity entity = entityManager.find(MyEntity.class, entityId);
            assertThat(entity).isNotNull();
            assertThat(Hibernate.isPropertyInitialized(entity, "eagerProperty1")).isTrue();
            assertThat(Hibernate.isPropertyInitialized(entity, "lazyProperty1")).isFalse();
            assertThat(Hibernate.isPropertyInitialized(entity, "lazyProperty2")).isFalse();

            assertThat(entity.eagerProperty1).isEqualTo(expectedEagerProperty1);
            assertThat(Hibernate.isPropertyInitialized(entity, "eagerProperty1")).isTrue();
            assertThat(Hibernate.isPropertyInitialized(entity, "lazyProperty1")).isFalse();
            assertThat(Hibernate.isPropertyInitialized(entity, "lazyProperty2")).isFalse();

            assertThat(entity.lazyProperty1).isEqualTo(expectedLazyProperty1);
            assertThat(Hibernate.isPropertyInitialized(entity, "eagerProperty1")).isTrue();
            assertThat(Hibernate.isPropertyInitialized(entity, "lazyProperty1")).isTrue();
            assertThat(Hibernate.isPropertyInitialized(entity, "lazyProperty2")).isTrue();

            assertThat(entity.lazyProperty2).isEqualTo(expectedLazyProperty2);
            assertThat(Hibernate.isPropertyInitialized(entity, "eagerProperty1")).isTrue();
            assertThat(Hibernate.isPropertyInitialized(entity, "lazyProperty1")).isTrue();
            assertThat(Hibernate.isPropertyInitialized(entity, "lazyProperty2")).isTrue();
        }
    }
}
