package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitCdiEntityManagerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void test() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        entityManager.persist(defaultEntity);

        DefaultEntity savedDefaultEntity = entityManager.find(DefaultEntity.class, defaultEntity.getId());
        assertEquals(defaultEntity.getName(), savedDefaultEntity.getName());
    }

}
