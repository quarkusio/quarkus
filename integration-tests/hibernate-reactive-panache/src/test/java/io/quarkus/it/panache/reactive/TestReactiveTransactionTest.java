package io.quarkus.it.panache.reactive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.UniAsserter;

@QuarkusTest
@TestReactiveTransaction
public class TestReactiveTransactionTest {

    @Test
    public void testTestTransaction(UniAsserter asserter) {
        asserter.assertNotNull(() -> Panache.currentTransaction());
    }

    @BeforeEach
    public void beforeEach(UniAsserter asserter) {
        asserter.assertNotNull(() -> Panache.currentTransaction());
    }
}
