package io.quarkus.it.panache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that @TestTransaction works as expected
 */
@QuarkusTest
@TestTransaction
public class TestTransactionTest {

    @Test
    @TestTransaction
    public void test1() {
        Assertions.assertEquals(0, Beer.find("name", "Lager").count());
        Beer b = new Beer();
        b.name = "Lager";
        Beer.persist(b);
    }

    @Test
    @TestTransaction
    public void test2() {
        Assertions.assertEquals(0, Beer.find("name", "Lager").count());
        Beer b = new Beer();
        b.name = "Lager";
        Beer.persist(b);
    }
}
