package io.quarkus.it.main;

import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TransactionalTestMethodTestCase {

    @Inject
    UserTransaction userTransaction;

    @Test
    @Transactional
    public void testUserTransaction() throws Exception {
        Assertions.assertEquals(Status.STATUS_ACTIVE, userTransaction.getStatus());
    }

}
