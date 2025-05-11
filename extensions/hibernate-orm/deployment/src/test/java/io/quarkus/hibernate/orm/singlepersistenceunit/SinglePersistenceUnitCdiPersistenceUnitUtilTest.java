package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitCdiPersistenceUnitUtilTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    PersistenceUnitUtil persistenceUnitUtil;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void testPersistenceUtil() {
        assertNotNull(persistenceUnitUtil);
        DefaultEntity entity = new DefaultEntity("test");
        entityManager.persist(entity);
        assertTrue(persistenceUnitUtil.isLoaded(entity, "name"));
    }

}
