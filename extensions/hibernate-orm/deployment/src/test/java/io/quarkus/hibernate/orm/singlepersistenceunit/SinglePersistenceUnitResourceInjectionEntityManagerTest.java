package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitResourceInjectionEntityManagerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @PersistenceContext
    EntityManager entityManager;

    @Test
    @Transactional
    public void test() {
        DefaultEntity entity = new DefaultEntity("gsmet");
        entityManager.persist(entity);

        DefaultEntity savedEntity = entityManager.find(DefaultEntity.class, entity.getId());
        assertEquals(entity.getName(), savedEntity.getName());
    }

}
