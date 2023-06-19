package io.quarkus.hibernate.orm.devui;

import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;

/**
 * All tests test the same api call, with different configuration and different expected results
 * This abstract class reduce the code in each test
 */
public abstract class DevUIHibernateOrmTest extends DevUIJsonRPCTest {

    private String expectedPersistenceUnitName = null;
    private String expectedTableName = null;
    private String expectedClassName = null;
    private int expectedNumberOfEntityTypes = 1;
    private int expectedNumberOfPersistenceUnits = 1;

    public DevUIHibernateOrmTest(String expectedPersistenceUnitName, String expectedTableName, String expectedClassName) {
        this.expectedPersistenceUnitName = expectedPersistenceUnitName;
        this.expectedTableName = expectedTableName;
        this.expectedClassName = expectedClassName;
    }

    public DevUIHibernateOrmTest(int expectedNumberOfEntityTypes,
            int expectedNumberOfPersistenceUnits) {
        this.expectedNumberOfEntityTypes = expectedNumberOfEntityTypes;
        this.expectedNumberOfPersistenceUnits = expectedNumberOfPersistenceUnits;
    }

    @Test
    public void testGetInfo() throws Exception {
        JsonNode getInfoResponse = super.executeJsonRPCMethod("getInfo");
        Assertions.assertNotNull(getInfoResponse);

        JsonNode persistenceUnits = getInfoResponse.get("persistenceUnits");
        Assertions.assertNotNull(persistenceUnits);
        Assertions.assertTrue(persistenceUnits.isArray());

        if (expectedPersistenceUnitName == null) {
            Assertions.assertEquals(0, persistenceUnits.size());
        } else {
            Assertions.assertEquals(1, persistenceUnits.size());
            Iterator<JsonNode> persistenceUnitsIterator = persistenceUnits.elements();
            while (persistenceUnitsIterator.hasNext()) {
                JsonNode defaultPersistenceUnit = persistenceUnitsIterator.next();
                JsonNode name = defaultPersistenceUnit.get("name");
                Assertions.assertEquals(expectedPersistenceUnitName, name.asText());

                JsonNode managedEntities = defaultPersistenceUnit.get("managedEntities");
                Assertions.assertNotNull(managedEntities);
                Assertions.assertTrue(managedEntities.isArray());

                Iterator<JsonNode> managedEntitiesIterator = managedEntities.elements();
                while (managedEntitiesIterator.hasNext()) {
                    JsonNode myEntity = managedEntitiesIterator.next();
                    Assertions.assertEquals(expectedNumberOfPersistenceUnits, persistenceUnits.size());

                    String tableName = myEntity.get("tableName").asText();
                    Assertions.assertEquals(expectedTableName, tableName);

                    String className = myEntity.get("className").asText();
                    Assertions.assertEquals(expectedClassName, className);

                }

                JsonNode namedQueries = defaultPersistenceUnit.get("namedQueries");
                Assertions.assertNotNull(namedQueries);
                Assertions.assertTrue(namedQueries.isArray());

            }
        }
    }

    @Test
    public void testGetNumberOfPersistenceUnits() throws Exception {
        JsonNode getNumberOfPersistenceUnitsResponse = super.executeJsonRPCMethod("getNumberOfPersistenceUnits");
        Assertions.assertNotNull(getNumberOfPersistenceUnitsResponse);
        Assertions.assertTrue(getNumberOfPersistenceUnitsResponse.isInt());
        Assertions.assertEquals(expectedNumberOfPersistenceUnits, getNumberOfPersistenceUnitsResponse.asInt());
    }

    @Test
    public void testGetNumberOfEntityTypes() throws Exception {
        JsonNode getNumberOfEntityTypesResponse = super.executeJsonRPCMethod("getNumberOfEntityTypes");
        Assertions.assertNotNull(getNumberOfEntityTypesResponse);
        Assertions.assertTrue(getNumberOfEntityTypesResponse.isInt());
        Assertions.assertEquals(expectedNumberOfEntityTypes, getNumberOfEntityTypesResponse.asInt());
    }

    @Test
    public void testGetNumberOfNamedQueries() throws Exception {
        JsonNode getNumberOfNamedQueriesResponse = super.executeJsonRPCMethod("getNumberOfNamedQueries");
        Assertions.assertNotNull(getNumberOfNamedQueriesResponse);
        Assertions.assertTrue(getNumberOfNamedQueriesResponse.isInt());
        Assertions.assertEquals(0, getNumberOfNamedQueriesResponse.asInt());
    }

    @Override
    protected String getNamespace() {
        return "io.quarkus.quarkus-hibernate-orm";
    }

}
