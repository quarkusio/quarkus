package io.quarkus.it.panache;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that @TestTransaction works as expected when only used for a single test method
 */
@QuarkusTest
@TestMethodOrder(MethodName.class)
public class TestTransactionOnSingleMethodTest {

    @Test
    @TestTransaction
    public void test1() {
        Assertions.assertEquals(0, Beer.find("name", "Lager").count());
        Beer b = new Beer();
        b.name = "Lager";
        Beer.persist(b);
    }

    @Test
    @Transactional
    public void test2() {
        Assertions.assertEquals(0, Beer.find("name", "Lager").count());
        Beer b = new Beer();
        b.name = "Lager";
        Beer.persist(b);
    }

    @Test
    @Transactional
    public void test3() {
        Assertions.assertEquals(1, Beer.find("name", "Lager").count());
        Beer.deleteAll();
    }
}
