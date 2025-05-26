package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.hibernate.relational.SchemaManager;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitSchemaManagerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"))
            .overrideConfigKey("quarkus.hibernate-orm.database.generation", "none");
    @Inject
    SchemaManager schemaManager;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void testSchemaManager() {
        assertNotNull(schemaManager);
        assertThrows(SchemaManagementException.class,
                () -> schemaManager.validateMappedObjects(),
                "Validation should fail if table is missing.");

        schemaManager.exportMappedObjects(true);

        assertDoesNotThrow(() -> schemaManager.validateMappedObjects(),
                "Validation should pass after exporting objects.");

        entityManager.createNativeQuery("DROP TABLE IF EXISTS DefaultEntity").executeUpdate();

        SchemaManagementException ex = assertThrows(SchemaManagementException.class,
                () -> schemaManager.validateMappedObjects(),
                "Validation should fail if table is missing.");
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("missing table"),
                "Exception message should indicate missing table.");

    }
}
