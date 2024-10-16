package io.quarkus.hibernate.search.orm.elasticsearch.test.devui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;

/**
 * All DevUI tests check the same api call, with different configuration and different expected results.
 * This abstract class reduces the code in each test.
 */
public abstract class AbstractDevUITest extends DevUIJsonRPCTest {

    private final String expectedPersistenceUnitName;
    private final String expectedClassName;

    public AbstractDevUITest(String expectedPersistenceUnitName, String expectedClassName) {
        super("io.quarkus.quarkus-hibernate-search-orm-elasticsearch");
        this.expectedPersistenceUnitName = expectedPersistenceUnitName;
        this.expectedClassName = expectedClassName;
    }

    @Test
    public void testGetInfo() throws Exception {
        JsonNode getInfoResponse = executeJsonRPCMethod("getInfo");
        assertNotNull(getInfoResponse);

        JsonNode numberOfIndexedEntities = getInfoResponse.get("numberOfIndexedEntities");
        assertNotNull(numberOfIndexedEntities);
        assertTrue(numberOfIndexedEntities.isInt());
        assertEquals(expectedClassName == null ? 0 : 1, numberOfIndexedEntities.asInt());

        JsonNode persistenceUnits = getInfoResponse.get("persistenceUnits");
        assertNotNull(persistenceUnits);
        assertTrue(persistenceUnits.isArray());

        if (expectedPersistenceUnitName == null) {
            assertEquals(0, persistenceUnits.size());
        } else {
            assertEquals(1, persistenceUnits.size());
            Iterator<JsonNode> persistenceUnitsIterator = persistenceUnits.elements();
            while (persistenceUnitsIterator.hasNext()) {
                JsonNode persistenceUnit = persistenceUnitsIterator.next();
                JsonNode name = persistenceUnit.get("name");
                assertEquals(expectedPersistenceUnitName, name.asText());

                JsonNode indexedEntities = persistenceUnit.get("indexedEntities");
                assertNotNull(indexedEntities);
                assertTrue(indexedEntities.isArray());

                Iterator<JsonNode> managedEntitiesIterator = indexedEntities.elements();
                while (managedEntitiesIterator.hasNext()) {
                    JsonNode myEntity = managedEntitiesIterator.next();
                    String javaClassName = myEntity.get("javaClass").asText();
                    assertEquals(expectedClassName, javaClassName);
                }
            }
        }
    }

    @Test
    public void testGetNumberOfPersistenceUnits() throws Exception {
        JsonNode getNumberOfPersistenceUnitsResponse = executeJsonRPCMethod("getNumberOfPersistenceUnits");
        assertNotNull(getNumberOfPersistenceUnitsResponse);
        assertTrue(getNumberOfPersistenceUnitsResponse.isInt());
        assertEquals(expectedPersistenceUnitName == null ? 0 : 1, getNumberOfPersistenceUnitsResponse.asInt());
    }

    @Test
    public void testGetNumberOfIndexedEntityTypes() throws Exception {
        JsonNode getNumberOfEntityTypesResponse = executeJsonRPCMethod("getNumberOfIndexedEntityTypes");
        assertNotNull(getNumberOfEntityTypesResponse);
        assertTrue(getNumberOfEntityTypesResponse.isInt());
        assertEquals(expectedClassName == null ? 0 : 1, getNumberOfEntityTypesResponse.asInt());
    }
}
