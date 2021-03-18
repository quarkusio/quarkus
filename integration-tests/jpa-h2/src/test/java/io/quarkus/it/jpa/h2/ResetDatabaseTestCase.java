package io.quarkus.it.jpa.h2;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.ResetDatabase;
import io.quarkus.test.junit.QuarkusTest;

//if the DB was not cleaned between method invocations then one of these tests would fail
@QuarkusTest
@ResetDatabase
@Transactional
public class ResetDatabaseTestCase {

    @Inject
    EntityManager entityManager;

    @Test
    public void test1() {
        Assertions.assertEquals(0, entityManager.createQuery("from Artwork").getResultList().size());
        Artwork d = new Artwork();
        d.setName("Mona Lisa");
        entityManager.persist(d);
    }

    @Test
    public void test2() {
        Assertions.assertEquals(0, entityManager.createQuery("from Artwork").getResultList().size());
        Artwork d = new Artwork();
        d.setName("Mona Lisa");
        entityManager.persist(d);
    }
}
