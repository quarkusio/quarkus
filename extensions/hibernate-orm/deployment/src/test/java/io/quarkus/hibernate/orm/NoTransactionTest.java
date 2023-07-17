package io.quarkus.hibernate.orm;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TransactionRequiredException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class NoTransactionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Address.class)
                    .addAsResource("application.properties"));

    @Inject
    EntityManager entityManager;

    @Test
    public void testNTransaction() {
        Arc.container().requestContext().activate();
        try {
            Assertions.assertThrows(TransactionRequiredException.class, () -> {
                Address a = new Address("test");
                entityManager.persist(a);
            });
        } finally {
            Arc.container().requestContext().terminate();
        }
    }

}
