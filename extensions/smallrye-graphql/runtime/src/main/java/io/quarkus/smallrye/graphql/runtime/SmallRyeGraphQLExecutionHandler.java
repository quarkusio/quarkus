package io.quarkus.smallrye.graphql.runtime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.smallrye.graphql.execution.ExecutionResponse;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that does the execution of GraphQL Requests
 */
public class SmallRyeGraphQLExecutionHandler extends SmallRyeGraphQLAbstractHandler {
    private boolean allowGet = false;
    private static final String QUERY = "query";
    private static final String VARIABLES = "variables";
    private static final String OK = "OK";

    private static final JsonBuilderFactory jsonObjectFactory = Json.createBuilderFactory(null);
    private static final JsonReaderFactory jsonReaderFactory = Json.createReaderFactory(null);

    public SmallRyeGraphQLExecutionHandler(boolean allowGet, CurrentIdentityAssociation currentIdentityAssociation,
            CurrentVertxRequest currentVertxRequest) {
        super(currentIdentityAssociation, currentVertxRequest);
        this.allowGet = allowGet;
    }

    @Override
    protected void doHandle(final RoutingContext ctx) {

        HttpServerRequest request = ctx.request();
        HttpServerResponse response = ctx.response();

        response.headers().set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");

        switch (request.method().name()) {
            case "OPTIONS":
                handleOptions(response);
                break;
            case "POST":
                handlePost(response, ctx);
                break;
            case "GET":
                handleGet(response, ctx);
                break;
            default:
                ctx.next();
                break;
        }
    }

    private void handleOptions(HttpServerResponse response) {
        response.headers().set(HttpHeaders.ALLOW, getAllowedMethods());
        response.setStatusCode(200).setStatusMessage(OK).end();
    }

    private void handlePost(HttpServerResponse response, RoutingContext ctx) {
        if (ctx.getBody() != null) {
            byte[] bytes = ctx.getBody().getBytes();
            String postResponse = doRequest(bytes);
            response.setStatusCode(200).setStatusMessage(OK).end(Buffer.buffer(postResponse));
        } else {
            response.setStatusCode(204).end();
        }
    }

    private void handleGet(HttpServerResponse response, RoutingContext ctx) {
        if (allowGet) {
            String query = getQueryParameter(ctx, QUERY);
            if (query != null && !query.isEmpty()) {
                try {
                    String variables = getQueryParameter(ctx, VARIABLES);

                    JsonObjectBuilder input = jsonObjectFactory.createObjectBuilder();
                    input.add(QUERY, URLDecoder.decode(query, "UTF8"));
                    if (variables != null && !variables.isEmpty()) {
                        JsonObject jsonObject = toJsonObject(URLDecoder.decode(variables, "UTF8"));
                        input.add(VARIABLES, jsonObject);
                    }

                    String getResponse = doRequest(input.build());

                    response.setStatusCode(200)
                            .setStatusMessage(OK)
                            .end(Buffer.buffer(getResponse));
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                response.setStatusCode(204).end();
            }
        } else {
            response.setStatusCode(405).end();
        }
    }

    private String getQueryParameter(RoutingContext ctx, String parameterName) {
        List<String> all = ctx.queryParam(parameterName);
        if (all != null && !all.isEmpty()) {
            return all.get(0);
        }
        return null;
    }

    private String getAllowedMethods() {
        if (allowGet) {
            return "GET, POST, OPTIONS";
        } else {
            return "POST, OPTIONS";
        }
    }

    private String doRequest(final byte[] body) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(body);
                final JsonReader jsonReader = jsonReaderFactory.createReader(input)) {
            JsonObject jsonInput = jsonReader.readObject();
            return doRequest(jsonInput);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String doRequest(JsonObject jsonInput) {
        ExecutionResponse executionResponse = getExecutionService().execute(jsonInput);
        if (executionResponse != null) {
            return executionResponse.getExecutionResultAsString();
        }
        return null;
    }

    private static JsonObject toJsonObject(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }

        try (JsonReader jsonReader = jsonReaderFactory.createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }
}
