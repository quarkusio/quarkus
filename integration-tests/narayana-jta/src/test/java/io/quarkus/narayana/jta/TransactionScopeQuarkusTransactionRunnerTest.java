package io.quarkus.narayana.jta;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TransactionScopeQuarkusTransactionRunnerTest {

    @Inject
    TransactionScopedBean beanTransactional;

    @Inject
    TransactionBeanWithEvents beanEvents;

    @Test
    void transactionScopedInTransaction() {
        TransactionScopedBean.resetCounters();

        QuarkusTransaction.requiringNew().run(() -> {
            beanTransactional.setValue(42);
            assertEquals(1, TransactionScopedBean.getInitializedCount(), "Expected @PostConstruct to be invoked");
            assertEquals(42, beanTransactional.getValue(), "Transaction scope did not save the value");

            QuarkusTransaction.suspendingExisting().run(() -> {
                assertThrows(ContextNotActiveException.class, () -> {
                    beanTransactional.getValue();
                }, "Not expecting to have available TransactionScoped bean outside of the transaction");

                QuarkusTransaction.requiringNew().run(() -> {
                    beanTransactional.setValue(1);
                    assertEquals(2, TransactionScopedBean.getInitializedCount(), "Expected @PostConstruct to be invoked");
                    assertEquals(1, beanTransactional.getValue(), "Transaction scope did not save the value");
                });
                assertEquals(1, TransactionScopedBean.getPreDestroyCount(), "Expected @PreDestroy to be invoked");

                assertThrows(ContextNotActiveException.class, () -> {
                    beanTransactional.getValue();
                }, "Not expecting to have available TransactionScoped bean outside of the transaction");
            });

            assertEquals(42, beanTransactional.getValue(), "Transaction scope did not resumed correctly");
        });
        assertEquals(2, TransactionScopedBean.getPreDestroyCount(), "Expected @PreDestroy to be invoked");
    }

    @Test
    void scopeEventsAreEmitted() {
        TransactionBeanWithEvents.cleanCounts();

        QuarkusTransaction.requiringNew().run(() -> {
            beanEvents.listenToCommitRollback();
        });

        assertEquals(1, TransactionBeanWithEvents.getInitialized(), "Expected @Initialized to be observed");
        assertEquals(1, TransactionBeanWithEvents.getBeforeDestroyed(), "Expected @BeforeDestroyed to be observed");
        assertEquals(1, TransactionBeanWithEvents.getDestroyed(), "Expected @Destroyed to be observed");
        assertEquals(1, TransactionBeanWithEvents.getCommited(), "Expected commit to be called once");
        assertEquals(0, TransactionBeanWithEvents.getRolledBack(), "Expected no rollback");
        TransactionBeanWithEvents.cleanCounts();

        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().run(() -> {
            beanEvents.listenToCommitRollback();
            throw new RuntimeException();
        }))
                // expect runtime exception to rollback the call
                .isInstanceOf(RuntimeException.class);

        assertEquals(1, TransactionBeanWithEvents.getInitialized(), "Expected @Initialized to be observed");
        assertEquals(1, TransactionBeanWithEvents.getBeforeDestroyed(), "Expected @BeforeDestroyed to be observed");
        assertEquals(1, TransactionBeanWithEvents.getDestroyed(), "Expected @Destroyed to be observed");
        assertEquals(0, TransactionBeanWithEvents.getCommited(), "Expected no commit");
        assertEquals(1, TransactionBeanWithEvents.getRolledBack(), "Expected rollback to be called once");
        TransactionBeanWithEvents.cleanCounts();
    }

}
