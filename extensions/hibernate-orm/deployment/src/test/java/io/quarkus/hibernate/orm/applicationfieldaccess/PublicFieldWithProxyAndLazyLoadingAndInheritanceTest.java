package io.quarkus.hibernate.orm.applicationfieldaccess;

import static io.quarkus.hibernate.orm.TransactionTestUtils.inTransaction;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.transaction.UserTransaction;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.TransactionTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class PublicFieldWithProxyAndLazyLoadingAndInheritanceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TransactionTestUtils.class)
                    .addClasses(Containing.class, Contained.class, ContainedExtended.class)
                    .addClass(FieldAccessEnhancedDelegate.class))
            .withConfigurationResource("application.properties");

    @Inject
    EntityManager em;

    @Inject
    UserTransaction transaction;

    private Long containingID;

    @Test
    public void test() {
        FieldAccessEnhancedDelegate delegate = new FieldAccessEnhancedDelegate();
        inTransaction(transaction, () -> {
            containingID = delegate.createEntities(em, "George");
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoading(em, containingID);
        });
    }

    @Entity(name = "Containing")
    public static class Containing {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        public Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        public Contained contained;
    }

    @Entity(name = "Contained")
    public static class Contained {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        public Long id;

        public String name;

        Contained() {
        }

        Contained(String name) {
            this.name = name;
        }
    }

    @Entity(name = "ContainedExtended")
    public static class ContainedExtended extends Contained {

        ContainedExtended() {
        }

        ContainedExtended(String name) {
            this.name = name;
        }

    }

    /**
     * A class whose bytecode is transformed by Quarkus to replace public field access with getter/setter access.
     * <p>
     * (Test bytecode was not transformed by Quarkus when using QuarkusUnitTest last time I checked).
     */
    private static class FieldAccessEnhancedDelegate {

        public long createEntities(EntityManager entityManager, String name) {
            Containing containing = new Containing();
            ContainedExtended contained = new ContainedExtended(name);
            containing.contained = contained;
            entityManager.persist(contained);
            entityManager.persist(containing);
            return containing.id;
        }

        public void testLazyLoading(EntityManager entityManager, Long containingID) {
            Containing containing = entityManager.find(Containing.class, containingID);
            Contained contained = containing.contained;
            assertThat(contained).isNotNull();
            assertThat(Hibernate.isPropertyInitialized(contained, "name")).isFalse();
            assertThat(contained.name).isNotNull();
        }
    }
}
