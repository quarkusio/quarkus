package io.quarkus.it.hibertnate.orm.graphql.panache;

import javax.json.Json;
import javax.json.JsonObject;

public class PayloadCreator {

    private PayloadCreator() {
    }

    public static String getPayload(String query) {
        JsonObject jsonObject = createRequestBody(query);
        return jsonObject.toString();
    }

    private static JsonObject createRequestBody(String graphQL) {
        return createRequestBody(graphQL, null);
    }

    private static JsonObject createRequestBody(String graphQL, JsonObject variables) {
        // Create the request
        if (variables == null || variables.isEmpty()) {
            variables = Json.createObjectBuilder().build();
        }
        return Json.createObjectBuilder().add(QUERY, graphQL).add(VARIABLES, variables).build();
    }

    public static final String MEDIATYPE_JSON = "application/json";
    private static final String QUERY = "query";
    private static final String VARIABLES = "variables";
}
