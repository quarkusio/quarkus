package io.quarkus.hibernate.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class UseNamedDatasourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application-named-datasource.properties", "application.properties"));

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void testPersistenceAndConfigTest() {
        MyEntity entity = new MyEntity("name");
        entityManager.persist(entity);

        MyEntity savedEntity = entityManager.find(MyEntity.class, entity.getId());
        assertEquals(entity.getName(), savedEntity.getName());
    }

}
