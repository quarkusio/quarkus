package io.quarkus.narayana.jta;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TransactionScopeTransactionalTest {
    @Inject
    TransactionBeanWithEvents beanEvents;

    @Test
    void scopeEventsAreEmitted() {
        TransactionBeanWithEvents.cleanCounts();

        beanEvents.doInTransaction(true);

        assertEquals(1, TransactionBeanWithEvents.getInitialized(), "Expected @Initialized to be observed");
        assertEquals(1, TransactionBeanWithEvents.getBeforeDestroyed(), "Expected @BeforeDestroyed to be observed");
        assertEquals(1, TransactionBeanWithEvents.getDestroyed(), "Expected @Destroyed to be observed");
        assertEquals(1, TransactionBeanWithEvents.getCommited(), "Expected commit to be called once");
        assertEquals(0, TransactionBeanWithEvents.getRolledBack(), "Expected no rollback");
        TransactionBeanWithEvents.cleanCounts();

        assertThatThrownBy(() -> beanEvents.doInTransaction(false))
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
