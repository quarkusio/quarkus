package io.quarkus.it.panache.reactive;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.hibernate.reactive.panache.TransactionalUniAsserter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;

@QuarkusTest
public class TransactionalUniAsserterTest {

    private static final AtomicBoolean ASSERTER_USED = new AtomicBoolean();

    @RunOnVertxContext
    @Test
    public void testTransactionalUniAsserter(TransactionalUniAsserter asserter) {
        assertNotNull(asserter);
        asserter.assertThat(Panache::currentTransaction, transaction -> {
            ASSERTER_USED.set(true);
            assertNotNull(transaction);
            assertFalse(transaction.isMarkedForRollback());
            asserter.putData("tx", transaction);
        });
        asserter.assertThat(Panache::currentTransaction, transaction -> {
            assertNotNull(transaction);
            assertFalse(transaction.isMarkedForRollback());
            assertNotEquals(transaction, asserter.getData("tx"));
        });
    }

    @AfterAll
    static void afterAll() {
        assertTrue(ASSERTER_USED.get());
    }

}
