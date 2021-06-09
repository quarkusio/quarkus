package io.quarkus.it.panache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that @TestTransaction works as expected when used for the entire class
 */
@QuarkusTest
@TestTransaction
@TestMethodOrder(MethodName.class)
public class TestTransactionTest {

    @Test
    public void test1() {
        Assertions.assertEquals(0, Beer.find("name", "Lager").count());
        Beer b = new Beer();
        b.name = "Lager";
        Beer.persist(b);
    }

    @Test
    public void test2() {
        Assertions.assertEquals(0, Beer.find("name", "Lager").count());
        // interceptor must not choke on this self-intercepted non-test method invocation
        intentionallyNonPrivateHelperMethod();
        Beer b = new Beer();
        b.name = "Lager";
        Beer.persist(b);
    }

    @Test
    public void test3() {
        Assertions.assertEquals(0, Beer.find("name", "Lager").count());
    }

    void intentionallyNonPrivateHelperMethod() {
    }
}
