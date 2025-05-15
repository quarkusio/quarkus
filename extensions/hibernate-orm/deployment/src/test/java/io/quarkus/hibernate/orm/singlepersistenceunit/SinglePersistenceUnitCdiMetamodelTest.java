package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitCdiMetamodelTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    Metamodel metamodel;

    @Test
    public void testMetamodel() {
        assertNotNull(metamodel);
        EntityType<DefaultEntity> entityType = metamodel.entity(DefaultEntity.class);
        assertNotNull(entityType);
        assertTrue(
                metamodel.getEntities().stream()
                        .anyMatch(et -> et.getJavaType().equals(DefaultEntity.class)),
                "Metamodel should contain DefaultEntity");
        assertEquals(DefaultEntity.class.getSimpleName(), entityType.getName());
    }

}
