package io.quarkus.virtual.graphql;

import static io.quarkus.jsonp.JsonProviderHolder.jsonProvider;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

import org.hamcrest.CoreMatchers;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Some shared methods
 */
public abstract class AbstractGraphQLTest {

    static {
        RestAssured.registerParser("application/graphql+json", Parser.JSON);
    }

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
        return getPayload(query, null);
    }

    protected String getPayload(String query, String variables) {
        JsonObject jsonObject = createRequestBody(query, variables);
        return jsonObject.toString();
    }

    protected JsonObject createRequestBody(String graphQL, String variables) {
        // Create the request
        JsonObject vjo = jsonProvider().createObjectBuilder().build();
        if (variables != null && !variables.isEmpty()) {
            try (JsonReader jsonReader = jsonProvider().createReader(new StringReader(variables))) {
                vjo = jsonReader.readObject();
            }
        }

        JsonObjectBuilder job = jsonProvider().createObjectBuilder();
        if (graphQL != null && !graphQL.isEmpty()) {
            job.add(QUERY, graphQL);
        }

        return job.add(VARIABLES, vjo).build();
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

    /**
     * Hold info about a thread
     */
    public static class TestThread {

        private long id;
        private String name;
        private int priority;
        private String state;
        private String group;

        public TestThread() {
            super();
        }

        public TestThread(long id, String name, int priority, String state, String group) {
            this.id = id;
            this.name = name;
            this.priority = priority;
            this.state = state;
            this.group = group;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getVertxContextClassName() {
            Context vc = Vertx.currentContext();
            return vc.getClass().getName();
        }
    }
}
