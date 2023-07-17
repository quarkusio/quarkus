package io.quarkus.hibernate.orm.panache.deployment.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class EntityManagerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(MyEntity.class));

    @Test
    void entityManagerShouldExist() {
        MyEntity entity = new MyEntity();
        assertNotNull(entity.getEntityManager());
    }

}
