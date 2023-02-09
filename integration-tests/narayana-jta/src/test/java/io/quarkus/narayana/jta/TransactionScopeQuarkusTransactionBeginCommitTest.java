package io.quarkus.narayana.jta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Inject;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TransactionScopeQuarkusTransactionBeginCommitTest {

    @Inject
    TransactionManager tm;

    @Inject
    TransactionScopedBean beanTransactional;

    @Inject
    TransactionBeanWithEvents beanEvents;

    @Test
    void transactionScopedInTransaction() throws Exception {
        TransactionScopedBean.resetCounters();

        QuarkusTransaction.begin();
        beanTransactional.setValue(42);
        assertEquals(1, TransactionScopedBean.getInitializedCount(), "Expected @PostConstruct to be invoked");
        assertEquals(42, beanTransactional.getValue(), "Transaction scope did not save the value");
        Transaction suspendedTransaction = tm.suspend();

        assertThrows(ContextNotActiveException.class, () -> {
            beanTransactional.getValue();
        }, "Not expecting to have available TransactionScoped bean outside of the transaction");

        QuarkusTransaction.begin();
        beanTransactional.setValue(1);
        assertEquals(2, TransactionScopedBean.getInitializedCount(), "Expected @PostConstruct to be invoked");
        assertEquals(1, beanTransactional.getValue(), "Transaction scope did not save the value");
        QuarkusTransaction.commit();
        assertEquals(1, TransactionScopedBean.getPreDestroyCount(), "Expected @PreDestroy to be invoked");

        assertThrows(ContextNotActiveException.class, () -> {
            beanTransactional.getValue();
        }, "Not expecting to have available TransactionScoped bean outside of the transaction");

        tm.resume(suspendedTransaction);
        assertEquals(42, beanTransactional.getValue(), "Transaction scope did not resumed correctly");
        QuarkusTransaction.rollback();
        assertEquals(2, TransactionScopedBean.getPreDestroyCount(), "Expected @PreDestroy to be invoked");
    }

    @Test
    void scopeEventsAreEmitted() {
        TransactionBeanWithEvents.cleanCounts();

        QuarkusTransaction.begin();
        beanEvents.listenToCommitRollback();
        QuarkusTransaction.commit();

        assertEquals(1, TransactionBeanWithEvents.getInitialized(), "Expected @Initialized to be observed");
        assertEquals(1, TransactionBeanWithEvents.getBeforeDestroyed(), "Expected @BeforeDestroyed to be observed");
        assertEquals(1, TransactionBeanWithEvents.getDestroyed(), "Expected @Destroyed to be observed");
        assertEquals(1, TransactionBeanWithEvents.getCommited(), "Expected commit to be called once");
        assertEquals(0, TransactionBeanWithEvents.getRolledBack(), "Expected no rollback");
        TransactionBeanWithEvents.cleanCounts();

        QuarkusTransaction.begin();
        beanEvents.listenToCommitRollback();
        QuarkusTransaction.rollback();

        assertEquals(1, TransactionBeanWithEvents.getInitialized(), "Expected @Initialized to be observed");
        assertEquals(1, TransactionBeanWithEvents.getBeforeDestroyed(), "Expected @BeforeDestroyed to be observed");
        assertEquals(1, TransactionBeanWithEvents.getDestroyed(), "Expected @Destroyed to be observed");
        assertEquals(0, TransactionBeanWithEvents.getCommited(), "Expected no commit");
        assertEquals(1, TransactionBeanWithEvents.getRolledBack(), "Expected rollback to be called once");
        TransactionBeanWithEvents.cleanCounts();
    }

}
