package io.quarkus.hibernate.orm.packages;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.TransactionTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class PackageLevelAnnotationWithExplicitPackagePropertyTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TransactionTestUtils.class)
                    .addPackage(PackageLevelAnnotationWithExplicitPackagePropertyTest.class.getPackage()))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.packages", "io.quarkus.hibernate.orm.packages");

    @Inject
    EntityManager entityManager;

    @Inject
    UserTransaction transaction;

    @Test
    public void test() {
        ParentEntity parent1 = new ParentEntity("parent1");

        inTransaction(() -> {
            entityManager.persist(parent1);
        });

        inTransaction(() -> {
            final List<ParentEntity> list = entityManager.createNamedQuery("test", ParentEntity.class)
                    .getResultList();
            assertThat(list.size()).isEqualTo(1);
        });
    }

    private void inTransaction(Runnable runnable) {
        TransactionTestUtils.inTransaction(transaction, runnable);
    }
}
