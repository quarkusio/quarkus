package io.quarkus.test.devui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;

/**
 * All tests test the same api call, with different configuration and different expected results
 * This abstract class reduce the code in each test
 */
public abstract class AbstractDevUIHibernateOrmTest extends DevUIJsonRPCTest {

    private final String expectedPersistenceUnitName;
    private final String expectedTableName;
    private final String expectedClassName;

    public AbstractDevUIHibernateOrmTest(String expectedPersistenceUnitName, String expectedTableName,
            String expectedClassName) {
        super("io.quarkus.quarkus-hibernate-orm");
        this.expectedPersistenceUnitName = expectedPersistenceUnitName;
        this.expectedTableName = expectedTableName;
        this.expectedClassName = expectedClassName;
    }

    @Test
    public void testGetInfo() throws Exception {
        JsonNode getInfoResponse = super.executeJsonRPCMethod("getInfo");
        assertNotNull(getInfoResponse);

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

                JsonNode managedEntities = persistenceUnit.get("managedEntities");
                assertNotNull(managedEntities);
                assertTrue(managedEntities.isArray());

                Iterator<JsonNode> managedEntitiesIterator = managedEntities.elements();
                while (managedEntitiesIterator.hasNext()) {
                    JsonNode myEntity = managedEntitiesIterator.next();

                    String tableName = myEntity.get("tableName").asText();
                    assertEquals(expectedTableName, tableName);

                    String className = myEntity.get("className").asText();
                    assertEquals(expectedClassName, className);

                }

                JsonNode namedQueries = persistenceUnit.get("namedQueries");
                assertNotNull(namedQueries);
                assertTrue(namedQueries.isArray());
            }
        }
    }

    @Test
    public void testGetNumberOfPersistenceUnits() throws Exception {
        JsonNode getNumberOfPersistenceUnitsResponse = super.executeJsonRPCMethod("getNumberOfPersistenceUnits");
        assertNotNull(getNumberOfPersistenceUnitsResponse);
        assertTrue(getNumberOfPersistenceUnitsResponse.isInt());
        assertEquals(expectedPersistenceUnitName == null ? 0 : 1, getNumberOfPersistenceUnitsResponse.asInt());
    }

    @Test
    public void testGetNumberOfEntityTypes() throws Exception {
        JsonNode getNumberOfEntityTypesResponse = super.executeJsonRPCMethod("getNumberOfEntityTypes");
        assertNotNull(getNumberOfEntityTypesResponse);
        assertTrue(getNumberOfEntityTypesResponse.isInt());
        assertEquals(expectedClassName == null ? 0 : 1, getNumberOfEntityTypesResponse.asInt());
    }

    @Test
    public void testGetNumberOfNamedQueries() throws Exception {
        JsonNode getNumberOfNamedQueriesResponse = super.executeJsonRPCMethod("getNumberOfNamedQueries");
        assertNotNull(getNumberOfNamedQueriesResponse);
        assertTrue(getNumberOfNamedQueriesResponse.isInt());
        assertEquals(0, getNumberOfNamedQueriesResponse.asInt());
    }
}
