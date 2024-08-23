package io.quarkus.hibernate.search.standalone.elasticsearch.test.devui;

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

    private final String expectedClassName;

    public AbstractDevUITest(String expectedClassName) {
        super("io.quarkus.quarkus-hibernate-search-standalone-elasticsearch");
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

        JsonNode indexedEntities = getInfoResponse.get("indexedEntities");
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
