package io.quarkus.test.devui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.Map;

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
    private final Integer expectedResults;
    private final boolean reactive;

    public AbstractDevUIHibernateOrmTest(String expectedPersistenceUnitName, String expectedTableName,
            String expectedClassName, Integer expectedResults, boolean reactive) {
        super("io.quarkus.quarkus-hibernate-orm");
        this.expectedPersistenceUnitName = expectedPersistenceUnitName;
        this.expectedTableName = expectedTableName;
        this.expectedClassName = expectedClassName;
        this.expectedResults = expectedResults;
        this.reactive = reactive;
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

                JsonNode reactive = persistenceUnit.get("reactive");
                assertTrue(reactive.isBoolean());
                assertEquals(this.reactive, reactive.asBoolean());
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

    @Test
    public void testExecuteHQL() throws Exception {
        String entityName = expectedTableName != null ? expectedTableName : "MyEntity";
        Map<String, Object> arguments = Map.of(
                "hql", "select e from " + entityName + " e where e.id = 1",
                "persistenceUnit", expectedPersistenceUnitName != null ? expectedPersistenceUnitName : "",
                "pageNumber", 1,
                "pageSize", 15);

        JsonNode dataSet = super.executeJsonRPCMethod("executeHQL", arguments);

        if (expectedResults != null) {
            // Expect number of results
            assertNotNull(dataSet);
            assertTrue(dataSet.has("totalNumberOfElements"));
            assertTrue(dataSet.has("data"));
            assertFalse(dataSet.has("error"));

            JsonNode elements = dataSet.get("totalNumberOfElements");
            assertTrue(elements.isNumber());
            assertEquals(expectedResults, elements.intValue());

            JsonNode data = dataSet.get("data");
            assertTrue(data.isArray());
            assertEquals(expectedResults, data.size());
            for (int i = 1; i <= expectedResults; i++) {
                JsonNode element = data.get(i - 1);
                assertEquals(i, element.get("id").intValue());
                assertEquals("entity_" + i, data.get(0).get("field").textValue());
            }
        } else if (expectedPersistenceUnitName != null) {
            // Expecting an empty result set
            assertNotNull(dataSet);
            assertTrue(dataSet.has("totalNumberOfElements"));
            assertTrue(dataSet.has("data"));
            assertFalse(dataSet.has("error"));

            JsonNode elements = dataSet.get("totalNumberOfElements");
            assertTrue(elements.isNumber());
            assertEquals(0, elements.intValue());

            JsonNode data = dataSet.get("data");
            assertTrue(data.isArray());
            assertEquals(0, data.size());
        } else {
            // Expecting an error
            assertNotNull(dataSet);
            assertTrue(dataSet.has("totalNumberOfElements"));
            assertFalse(dataSet.has("data"));
            assertTrue(dataSet.has("error"));

            JsonNode elements = dataSet.get("totalNumberOfElements");
            assertTrue(elements.isNumber());
            assertEquals(-1, elements.intValue());

            JsonNode error = dataSet.get("error");
            assertTrue(error.isTextual());
            assertTrue(error.asText().contains("No such persistence unit"));
        }
    }
}
