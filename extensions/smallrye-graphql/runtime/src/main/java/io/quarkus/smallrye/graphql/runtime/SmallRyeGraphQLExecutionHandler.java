package io.quarkus.smallrye.graphql.runtime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.graphql.execution.ExecutionService;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that does the execution of GraphQL Requests
 */
public class SmallRyeGraphQLExecutionHandler implements Handler<RoutingContext> {
    private static boolean allowGet = false;
    private static final String QUERY = "query";
    private static final String OK = "OK";
    private volatile ExecutionService executionService;
    private final CurrentIdentityAssociation currentIdentityAssociation;

    public SmallRyeGraphQLExecutionHandler(boolean allowGet, CurrentIdentityAssociation currentIdentityAssociation) {
        this.allowGet = allowGet;
        this.currentIdentityAssociation = currentIdentityAssociation;
    }

    @Override
    public void handle(final RoutingContext ctx) {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            doHandle(ctx);
        } else {
            try {
                requestContext.activate();
                doHandle(ctx);
            } finally {
                requestContext.terminate();
            }
        }
    }

    private void doHandle(final RoutingContext ctx) {
        if (currentIdentityAssociation != null) {
            currentIdentityAssociation.setIdentity(QuarkusHttpUser.getSecurityIdentity(ctx, null));
        }

        HttpServerRequest request = ctx.request();
        HttpServerResponse response = ctx.response();

        response.headers().set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");

        switch (request.method()) {
            case OPTIONS:
                handleOptions(response);
                break;
            case POST:
                handlePost(response, ctx);
                break;
            case GET:
                handleGet(response, ctx);
                break;
            default:
                response.setStatusCode(405).end();
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
            List<String> queries = ctx.queryParam(QUERY);
            if (queries != null && !queries.isEmpty()) {
                String graphqlGetRequest = queries.get(0);
                String getResponse = doRequest(graphqlGetRequest.getBytes(StandardCharsets.UTF_8));
                response.setStatusCode(200)
                        .setStatusMessage(OK)
                        .end(Buffer.buffer(getResponse));
            } else {
                response.setStatusCode(204).end();
            }
        } else {
            response.setStatusCode(405).end();
        }
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
                final JsonReader jsonReader = Json.createReader(input)) {
            JsonObject jsonInput = jsonReader.readObject();
            JsonObject outputJson = getExecutionService().execute(jsonInput);
            if (outputJson != null) {
                try (StringWriter output = new StringWriter();
                        final JsonWriter jsonWriter = Json.createWriter(output)) {
                    jsonWriter.writeObject(outputJson);
                    output.flush();
                    return output.toString();
                }
            }
            return null;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ExecutionService getExecutionService() {
        if (this.executionService == null) {
            this.executionService = Arc.container().instance(ExecutionService.class).get();
        }
        return this.executionService;
    }
}
