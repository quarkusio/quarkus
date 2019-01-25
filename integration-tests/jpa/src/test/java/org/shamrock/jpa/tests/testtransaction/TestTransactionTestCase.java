package org.shamrock.jpa.tests.testtransaction;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.jboss.shamrock.test.dataaccess.TestTransaction;
import org.jboss.shamrock.test.junit.ShamrockTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.shamrock.jpa.tests.configurationless.Cake;

/**
 * This is a test of the TestTransaction JPA functionality
 * <p>
 * It is not in the TestTransaction module to make sure that it works in a real scenario
 * when what is being tested is not in the same module
 */
@ShamrockTest
@TestTransaction
public class TestTransactionTestCase {

    @Inject
    EntityManager entityManager;

    @Inject
    UserTransaction userTransaction;

    //we have two tests that both add cakes, but also assert that the DB is empty
    //if the transaction was not rolled back then one of these tests would fail, as the Cake from
    //the previous test would be there
    @Test
    void testTxRolledBack1() throws Exception {
        Assertions.assertEquals(Status.STATUS_ACTIVE, userTransaction.getStatus());
        Assertions.assertTrue(entityManager.createQuery("from Cake").getResultList().isEmpty());
        Cake c = new Cake();
        c.setType("Chocolate");
        entityManager.persist(c);
    }

    @Test
    void testTxRolledBack2() throws Exception {
        Assertions.assertEquals(Status.STATUS_ACTIVE, userTransaction.getStatus());
        Assertions.assertTrue(entityManager.createQuery("from Cake").getResultList().isEmpty());
        Cake c = new Cake();
        c.setType("Ice Cream");
        entityManager.persist(c);
    }
}
