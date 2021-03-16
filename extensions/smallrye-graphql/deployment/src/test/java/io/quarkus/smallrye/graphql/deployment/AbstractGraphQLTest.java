package io.quarkus.smallrye.graphql.deployment;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;

import io.restassured.RestAssured;

/**
 * Some shared methods
 */
public abstract class AbstractGraphQLTest {

    protected void pingTest() {
        pingPongTest("ping", "pong");
    }

    protected void pongTest() {
        pingPongTest("pong", "ping");
    }

    private void pingPongTest(String operationName, String message) {
        String pingRequest = getPayload("{\n" +
                "  " + operationName + " {\n" +
                "    message\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(pingRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString("{\"data\":{\"" + operationName + "\":{\"message\":\"" + message + "\"}}}"));
    }

    protected String getPayload(String query) {
        JsonObject jsonObject = createRequestBody(query);
        return jsonObject.toString();
    }

    protected JsonObject createRequestBody(String graphQL) {
        return createRequestBody(graphQL, null);
    }

    protected JsonObject createRequestBody(String graphQL, JsonObject variables) {
        // Create the request
        if (variables == null || variables.isEmpty()) {
            variables = Json.createObjectBuilder().build();
        }
        return Json.createObjectBuilder().add(QUERY, graphQL).add(VARIABLES, variables).build();
    }

    protected static String getPropertyAsString() {
        return getPropertyAsString(null);
    }

    protected static String getPropertyAsString(Map<String, String> otherProperties) {
        try {
            Properties p = new Properties();
            p.putAll(PROPERTIES);
            StringWriter writer = new StringWriter();
            if (otherProperties != null) {
                p.putAll(otherProperties);
            }
            p.store(writer, "Test Properties");
            return writer.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected static final String MEDIATYPE_JSON = "application/json";
    protected static final String MEDIATYPE_TEXT = "text/plain";
    protected static final String QUERY = "query";
    protected static final String VARIABLES = "variables";

    protected static final Properties PROPERTIES = new Properties();
    static {
        PROPERTIES.put("smallrye.graphql.allowGet", "true");
        PROPERTIES.put("smallrye.graphql.printDataFetcherException", "true");
        PROPERTIES.put("smallrye.graphql.events.enabled", "true");
    }
}
