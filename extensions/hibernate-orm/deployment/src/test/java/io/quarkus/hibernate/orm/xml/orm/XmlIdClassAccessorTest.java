package io.quarkus.hibernate.orm.xml.orm;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.accessor.GeneratedAccessor;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusExtensionTest;

public class XmlIdClassAccessorTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Purchase.class, PurchaseId.class)
                    .addAsResource("accessor-guide/orm.xml", "META-INF/orm.xml"))
            .overrideConfigKey("quarkus.hibernate-accessor.strategy", "generated");

    @Inject
    EntityManager entityManager;

    @Test
    void loadsEntityByXmlMappedId() {
        QuarkusTransaction.requiringNew().run(() -> {
            Purchase purchase = new Purchase();
            purchase.shopId = 1;
            purchase.purchaseNumber = 2;
            entityManager.persist(purchase);
        });
        QuarkusTransaction.requiringNew().run(() -> {
            Purchase purchase = entityManager.find(Purchase.class, new PurchaseId(1, 2));
            assertThat(purchase).isNotNull();
            assertThat(entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(purchase))
                    .isEqualTo(new PurchaseId(1, 2));
        });
    }

    public static class Purchase {
        private int shopId;
        private int purchaseNumber;
    }

    @GeneratedAccessor
    public record PurchaseId(int shopId, int purchaseNumber) {
    }
}
