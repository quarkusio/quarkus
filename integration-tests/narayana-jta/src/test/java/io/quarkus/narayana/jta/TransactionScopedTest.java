package io.quarkus.narayana.jta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.enterprise.context.ContextNotActiveException;
import javax.inject.Inject;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TransactionScopedTest {
    @Inject
    private UserTransaction tx;

    @Inject
    private TransactionManager tm;

    @Inject
    private TransactionScopedBean beanTransactional;

    @Inject
    private TransactionBeanWithEvents beanEvents;

    @Test
    void transactionScopedInTransaction() throws Exception {
        tx.begin();
        beanTransactional.setValue(42);
        assertEquals(42, beanTransactional.getValue(), "Transaction scope did not save the value");
        Transaction suspendedTransaction = tm.suspend();

        assertThrows(ContextNotActiveException.class, () -> {
            beanTransactional.getValue();
        }, "Not expecting to have available TransactionScoped bean outside of the transaction");

        tx.begin();
        beanTransactional.setValue(1);
        assertEquals(1, beanTransactional.getValue(), "Transaction scope did not save the value");
        tx.commit();

        assertThrows(ContextNotActiveException.class, () -> {
            beanTransactional.getValue();
        }, "Not expecting to have available TransactionScoped bean outside of the transaction");

        tm.resume(suspendedTransaction);
        assertEquals(42, beanTransactional.getValue(), "Transaction scope did not resumed correctly");
        tx.rollback();
    }

    @Test
    void scopeEventsAreEmitted() {
        beanEvents.doInTransaction(true);

        try {
            beanEvents.doInTransaction(false);
        } catch (RuntimeException expected) {
            // expect runtime exception to rollback the call
        }

        assertEquals(2, beanEvents.getInitialized(), "Expected @Initialized to be observed");
        assertEquals(2, beanEvents.getBeforeDestroyed(), "Expected @BeforeDestroyed to be observer");
        assertEquals(2, beanEvents.getDestroyed(), "Expected @Destroyed to be observer");
        assertEquals(1, beanEvents.getCommited(), "Expected commit to be called once");
        assertEquals(1, beanEvents.getRolledBack(), "Expected rollback to be called once");
    }

}
