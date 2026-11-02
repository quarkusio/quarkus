package io.quarkus.it.panache.defaultpu;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.it.panache.custompu.CustomPuEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that @TestTransaction flushes every persistence unit before rolling back
 */
@QuarkusTest
@TestMethodOrder(MethodName.class)
public class TestTransactionFlushTest {

    static long defaultInsertCount;
    static long customInsertCount;

    @Inject
    SessionFactory sessionFactory;

    @Inject
    @PersistenceUnit("custom")
    SessionFactory customSessionFactory;

    @Test
    @TestTransaction
    public void test1() {
        defaultInsertCount = sessionFactory.getStatistics().getEntityInsertCount();
        Beer beer = new Beer();
        beer.name = "Flushed";
        Beer.persist(beer);
    }

    @Test
    @TestTransaction
    public void test2() {
        Assertions.assertEquals(defaultInsertCount + 1, sessionFactory.getStatistics().getEntityInsertCount());
        customInsertCount = customSessionFactory.getStatistics().getEntityInsertCount();
        CustomPuEntity entity = new CustomPuEntity();
        entity.string = "flushed";
        entity.persist();
    }

    @Test
    public void test3() {
        Assertions.assertEquals(customInsertCount + 1, customSessionFactory.getStatistics().getEntityInsertCount());
    }
}
