package io.quarkus.it.smallrye.graphql;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class PayloadCreator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PayloadCreator() {
    }

    public static String getPayload(String query) {
        ObjectNode node = createRequestBody(query);
        return node.toString();
    }

    private static ObjectNode createRequestBody(String graphQL) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put(QUERY, graphQL);
        node.putObject(VARIABLES);
        return node;
    }

    public static final String MEDIATYPE_JSON = "application/json";
    private static final String QUERY = "query";
    private static final String VARIABLES = "variables";
}
