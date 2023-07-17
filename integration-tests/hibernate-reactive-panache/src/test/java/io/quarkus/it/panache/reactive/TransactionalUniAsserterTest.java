package io.quarkus.it.panache.reactive;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.hibernate.reactive.panache.TransactionalUniAsserter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;

@QuarkusTest
public class TransactionalUniAsserterTest {

    @RunOnVertxContext
    @Test
    public void testTransactionalUniAsserter(TransactionalUniAsserter asserter) {
        assertNotNull(asserter);
        asserter.assertThat(() -> Panache.currentTransaction(), transaction -> {
            assertNotNull(transaction);
            assertFalse(transaction.isMarkedForRollback());
            asserter.putData("tx", transaction);
        });
        asserter.assertThat(() -> Panache.currentTransaction(), transaction -> {
            assertNotNull(transaction);
            assertFalse(transaction.isMarkedForRollback());
            assertNotEquals(transaction, asserter.getData("tx"));
        });
    }

}
