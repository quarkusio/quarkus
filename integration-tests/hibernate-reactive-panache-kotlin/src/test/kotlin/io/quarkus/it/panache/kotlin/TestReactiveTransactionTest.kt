package io.quarkus.it.panache.kotlin

import io.quarkus.it.panache.reactive.kotlin.Person
import io.quarkus.test.TestReactiveTransaction
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.vertx.UniAsserter
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class TestReactiveTransactionTest {

    @TestReactiveTransaction
    @Test
    fun testTestTransaction(asserter: UniAsserter) {
        asserter.assertNotNull { Person.currentTransaction() }
    }

    @TestReactiveTransaction
    @BeforeEach
    fun beforeEach(asserter: UniAsserter) {
        asserter.assertNotNull<Mutiny.Transaction> { io.quarkus.hibernate.reactive.panache.Panache.currentTransaction() }
    }
}