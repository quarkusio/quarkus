package io.quarkus.smallrye.graphql.runtime;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

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
    private boolean allowPostWithQueryParameters = false;
    private static final String QUERY = "query";
    private static final String OPERATION_NAME = "operationName";
    private static final String VARIABLES = "variables";
    private static final String EXTENSIONS = "extensions";
    private static final String APPLICATION_GRAPHQL = "application/graphql";
    private static final String OK = "OK";
    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/graphql+json; charset="
            + StandardCharsets.UTF_8.name();
    private static final String DEFAULT_REQUEST_CONTENT_TYPE = "application/json; charset="
            + StandardCharsets.UTF_8.name();

    public SmallRyeGraphQLExecutionHandler(boolean allowGet, boolean allowPostWithQueryParameters,
            CurrentIdentityAssociation currentIdentityAssociation,
            CurrentVertxRequest currentVertxRequest) {
        super(currentIdentityAssociation, currentVertxRequest);
        this.allowGet = allowGet;
        this.allowPostWithQueryParameters = allowPostWithQueryParameters;
    }

    @Override
    protected void doHandle(final RoutingContext ctx) {
        HttpServerRequest request = ctx.request();
        HttpServerResponse response = ctx.response();

        String accept = getRequestAccept(ctx);
        String requestedCharset = getCharset(accept);

        boolean isValid = isValidAcceptRequest(accept);

        if (!isValid) {
            handleInvalidAcceptRequest(response);
        } else {
            response.headers().set(HttpHeaders.CONTENT_TYPE, accept);

            switch (request.method().name()) {
                case "OPTIONS":
                    handleOptions(response);
                    break;
                case "POST":
                    handlePost(response, ctx, requestedCharset);
                    break;
                case "GET":
                    handleGet(response, ctx, requestedCharset);
                    break;
                default:
                    ctx.next();
                    break;
            }
        }
    }

    private void handleOptions(HttpServerResponse response) {
        response.headers().set(HttpHeaders.ALLOW, getAllowedMethods());
        response.setStatusCode(200).setStatusMessage(OK).end();
    }

    private void handlePost(HttpServerResponse response, RoutingContext ctx, String requestedCharset) {
        try {
            JsonObject jsonObjectFromBody = getJsonObjectFromBody(ctx);
            String postResponse;
            if (hasQueryParameters(ctx) && allowPostWithQueryParameters) {
                JsonObject jsonObjectFromQueryParameters = getJsonObjectFromQueryParameters(ctx);
                JsonObject mergedJsonObject = Json.createMergePatch(jsonObjectFromQueryParameters).apply(jsonObjectFromBody)
                        .asJsonObject();
                postResponse = doRequest(mergedJsonObject);
            } else {
                postResponse = doRequest(jsonObjectFromBody);
            }
            response.setStatusCode(200).setStatusMessage(OK).end(Buffer.buffer(postResponse, requestedCharset));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void handleGet(HttpServerResponse response, RoutingContext ctx, String requestedCharset) {
        if (allowGet) {
            try {
                JsonObject input = getJsonObjectFromQueryParameters(ctx);

                if (input.containsKey(QUERY)) {
                    String getResponse = doRequest(input);
                    response.setStatusCode(200)
                            .setStatusMessage(OK)
                            .end(Buffer.buffer(getResponse, requestedCharset));

                } else {
                    response.setStatusCode(204).end();
                }
            } catch (UnsupportedEncodingException uee) {
                throw new RuntimeException(uee);
            }
        } else {
            response.setStatusCode(405).end();
        }
    }

    private void handleInvalidAcceptRequest(HttpServerResponse response) {
        response.setStatusCode(406).end();
    }

    private JsonObject getJsonObjectFromQueryParameters(RoutingContext ctx) throws UnsupportedEncodingException {
        JsonObjectBuilder input = Json.createObjectBuilder();
        // Query
        String query = stripNewlinesAndTabs(readQueryParameter(ctx, QUERY));
        if (query != null && !query.isEmpty()) {
            input.add(QUERY, URLDecoder.decode(query, StandardCharsets.UTF_8.name()));
        }
        // OperationName
        String operationName = readQueryParameter(ctx, OPERATION_NAME);
        if (operationName != null && !operationName.isEmpty()) {
            input.add(OPERATION_NAME, URLDecoder.decode(query, StandardCharsets.UTF_8.name()));
        }

        // Variables
        String variables = stripNewlinesAndTabs(readQueryParameter(ctx, VARIABLES));
        if (variables != null && !variables.isEmpty()) {
            JsonObject jsonObject = toJsonObject(URLDecoder.decode(variables, StandardCharsets.UTF_8.name()));
            input.add(VARIABLES, jsonObject);
        }

        // Extensions
        String extensions = stripNewlinesAndTabs(readQueryParameter(ctx, EXTENSIONS));
        if (extensions != null && !extensions.isEmpty()) {
            JsonObject jsonObject = toJsonObject(URLDecoder.decode(extensions, StandardCharsets.UTF_8.name()));
            input.add(EXTENSIONS, jsonObject);
        }

        return input.build();
    }

    private JsonObject getJsonObjectFromBody(RoutingContext ctx) throws IOException {

        String contentType = getRequestContentType(ctx);
        String body = stripNewlinesAndTabs(readBody(ctx));

        // If the content type is application/graphql, the query is in the body
        if (contentType != null && contentType.startsWith(APPLICATION_GRAPHQL)) {
            JsonObjectBuilder input = Json.createObjectBuilder();
            input.add(QUERY, body);
            return input.build();
            // Else we expect a Json in the content    
        } else {
            try (StringReader bodyReader = new StringReader(body);
                    JsonReader jsonReader = jsonReaderFactory.createReader(bodyReader)) {
                return jsonReader.readObject();
            }
        }

    }

    private String readBody(RoutingContext ctx) {
        if (ctx.getBody() != null) {
            return ctx.getBodyAsString();
        }
        return null;
    }

    private String getRequestContentType(RoutingContext ctx) {
        String contentType = ctx.request().getHeader("Content-Type");
        if (contentType != null && !contentType.isEmpty() && !contentType.startsWith("*/*")) {
            return contentType;
        }
        return DEFAULT_REQUEST_CONTENT_TYPE;
    }

    private String getRequestAccept(RoutingContext ctx) {
        String accept = ctx.request().getHeader("Accept");
        if (accept != null && !accept.isEmpty() && !accept.startsWith("*/*")) {
            return accept;
        }
        return DEFAULT_RESPONSE_CONTENT_TYPE;
    }

    private String getCharset(String mimeType) {
        if (mimeType != null && mimeType.contains(";")) {
            String[] parts = mimeType.split(";");
            for (String part : parts) {
                if (part.trim().startsWith("charset")) {
                    return part.split("=")[1];
                }
            }
        }
        return StandardCharsets.UTF_8.name();
    }

    private boolean isValidAcceptRequest(String mimeType) {
        // At this point we only accept two 
        return mimeType.startsWith("application/json")
                || mimeType.startsWith("application/graphql+json");
    }

    private String readQueryParameter(RoutingContext ctx, String parameterName) {
        List<String> all = ctx.queryParam(parameterName);
        if (all != null && !all.isEmpty()) {
            return all.get(0);
        }
        return null;
    }

    private static final Pattern PATTERN_NEWLINE_OR_TAB = Pattern.compile("\\n|\\t");

    /**
     * Strip away unescaped tabs and line breaks from the incoming JSON document so that it can be
     * successfully parsed by a JSON parser.
     * This does NOT remove properly escaped \n and \t inside the document, just the raw characters (ASCII
     * values 9 and 10). Technically, this is not compliant with the JSON spec,
     * but we want to seamlessly support queries from Java text blocks, for example,
     * which preserve line breaks and tab characters.
     */
    private static String stripNewlinesAndTabs(final String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return PATTERN_NEWLINE_OR_TAB.matcher(input).replaceAll(" ");
    }

    private boolean hasQueryParameters(RoutingContext ctx) {
        return hasQueryParameter(ctx, QUERY) || hasQueryParameter(ctx, OPERATION_NAME) || hasQueryParameter(ctx, VARIABLES)
                || hasQueryParameter(ctx, EXTENSIONS);
    }

    private boolean hasQueryParameter(RoutingContext ctx, String parameterName) {
        List<String> all = ctx.queryParam(parameterName);
        if (all != null && !all.isEmpty()) {
            return true;
        }
        return false;
    }

    private String getAllowedMethods() {
        if (allowGet) {
            return "GET, POST, OPTIONS";
        } else {
            return "POST, OPTIONS";
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
