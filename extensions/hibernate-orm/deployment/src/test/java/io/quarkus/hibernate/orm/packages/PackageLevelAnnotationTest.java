package io.quarkus.hibernate.orm.packages;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class PackageLevelAnnotationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(PackageLevelAnnotationTest.class.getPackage())
                    .addAsResource("application.properties"));

    @Inject
    EntityManager entityManager;

    @Inject
    UserTransaction transaction;

    @Test
    public void test() {
        // If we get here, the package-level @AnyMetaDef was correctly detected:
        // otherwise, we would have had a failure on ORM bootstrap.

        ParentEntity parent1 = new ParentEntity("parent1");
        ParentEntity parent2 = new ParentEntity("parent2");
        ChildEntity1 child1 = new ChildEntity1("child1");
        ChildEntity2 child2 = new ChildEntity2("child2");
        parent1.setChild(child1);
        parent2.setChild(child2);

        inTransaction(() -> {
            entityManager.persist(child1);
            entityManager.persist(child2);
            entityManager.persist(parent1);
            entityManager.persist(parent2);
        });

        // Check that the @Any relation works correctly, just in case
        inTransaction(() -> {
            ParentEntity savedParent1 = entityManager.find(ParentEntity.class, parent1.getId());
            assertThat(savedParent1.getChild()).isInstanceOf(ChildEntity1.class);
            ParentEntity savedParent2 = entityManager.find(ParentEntity.class, parent2.getId());
            assertThat(savedParent2.getChild()).isInstanceOf(ChildEntity2.class);
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
}
