package io.quarkus.narayana.jta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Inject;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TransactionScopedTest {
    @Inject
    UserTransaction tx;

    @Inject
    TransactionManager tm;

    @Inject
    TransactionScopedBean beanTransactional;

    @Inject
    TransactionBeanWithEvents beanEvents;

    @Test
    void transactionScopedInTransaction() throws Exception {
        TransactionScopedBean.resetCounters();

        tx.begin();
        beanTransactional.setValue(42);
        assertEquals(1, TransactionScopedBean.getInitializedCount(), "Expected @PostConstruct to be invoked");
        assertEquals(42, beanTransactional.getValue(), "Transaction scope did not save the value");
        Transaction suspendedTransaction = tm.suspend();

        assertThrows(ContextNotActiveException.class, () -> {
            beanTransactional.getValue();
        }, "Not expecting to have available TransactionScoped bean outside of the transaction");

        tx.begin();
        beanTransactional.setValue(1);
        assertEquals(2, TransactionScopedBean.getInitializedCount(), "Expected @PostConstruct to be invoked");
        assertEquals(1, beanTransactional.getValue(), "Transaction scope did not save the value");
        tx.commit();
        assertEquals(1, TransactionScopedBean.getPreDestroyCount(), "Expected @PreDestroy to be invoked");

        assertThrows(ContextNotActiveException.class, () -> {
            beanTransactional.getValue();
        }, "Not expecting to have available TransactionScoped bean outside of the transaction");

        tm.resume(suspendedTransaction);
        assertEquals(42, beanTransactional.getValue(), "Transaction scope did not resumed correctly");
        tx.rollback();
        assertEquals(2, TransactionScopedBean.getPreDestroyCount(), "Expected @PreDestroy to be invoked");
    }

    @Test
    void scopeEventsAreEmitted() throws Exception {
        TransactionBeanWithEvents.cleanCounts();

        beanEvents.doInTransaction(true);

        try {
            beanEvents.doInTransaction(false);
        } catch (RuntimeException expected) {
            // expect runtime exception to rollback the call
        }

        tx.begin();
        tx.commit();

        assertEquals(3, TransactionBeanWithEvents.getInitialized(), "Expected @Initialized to be observed");
        assertEquals(3, TransactionBeanWithEvents.getBeforeDestroyed(), "Expected @BeforeDestroyed to be observer");
        assertEquals(3, TransactionBeanWithEvents.getDestroyed(), "Expected @Destroyed to be observer");
        assertEquals(1, TransactionBeanWithEvents.getCommited(), "Expected commit to be called once");
        assertEquals(1, TransactionBeanWithEvents.getRolledBack(), "Expected rollback to be called once");
    }

}
