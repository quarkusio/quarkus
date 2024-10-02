package io.quarkus.smallrye.graphql.runtime;

import static io.quarkus.jsonp.JsonProviderHolder.jsonProvider;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.stream.JsonParsingException;

import org.jboss.logging.Logger;

import graphql.ErrorType;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.AbortExecutionException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.smallrye.graphql.execution.ExecutionResponse;
import io.smallrye.graphql.execution.ExecutionResponseWriter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.ParsedHeaderValues;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that does the execution of GraphQL Requests
 */
public class SmallRyeGraphQLExecutionHandler extends SmallRyeGraphQLAbstractHandler {
    private final boolean allowGet;
    private final boolean allowPostWithQueryParameters;
    private static final String QUERY = "query";
    private static final String OPERATION_NAME = "operationName";
    private static final String VARIABLES = "variables";
    private static final String EXTENSIONS = "extensions";
    private static final String APPLICATION_GRAPHQL = "application/graphql";
    private static final String OK = "OK";
    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/graphql-response+json; charset="
            + StandardCharsets.UTF_8.name();
    private static final String DEFAULT_REQUEST_CONTENT_TYPE = "application/json; charset="
            + StandardCharsets.UTF_8.name();
    private static final String MISSING_OPERATION = "Missing operation body";

    private static final Logger log = Logger.getLogger(SmallRyeGraphQLExecutionHandler.class);

    public SmallRyeGraphQLExecutionHandler(boolean allowGet, boolean allowPostWithQueryParameters, boolean runBlocking,
            CurrentIdentityAssociation currentIdentityAssociation,
            CurrentVertxRequest currentVertxRequest) {
        super(currentIdentityAssociation, currentVertxRequest, runBlocking);
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
            if (hasQueryParameters(ctx) && allowPostWithQueryParameters) {
                JsonObject jsonObjectFromQueryParameters = getJsonObjectFromQueryParameters(ctx);
                JsonObject mergedJsonObject;
                if (jsonObjectFromBody != null) {
                    mergedJsonObject = jsonProvider().createMergePatch(jsonObjectFromQueryParameters).apply(jsonObjectFromBody)
                            .asJsonObject();
                } else {
                    mergedJsonObject = jsonObjectFromQueryParameters;
                }
                if (!mergedJsonObject.containsKey(QUERY)) {
                    response.setStatusCode(400).end(MISSING_OPERATION);
                    return;
                }
                doRequest(mergedJsonObject, response, ctx, requestedCharset);
            } else {
                if (jsonObjectFromBody == null) {
                    response.setStatusCode(400).end(MISSING_OPERATION);
                    return;
                }
                doRequest(jsonObjectFromBody, response, ctx, requestedCharset);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (JsonParsingException ex) {
            sendError("Unparseable request", response, ctx, requestedCharset);
        }
    }

    private void handleGet(HttpServerResponse response, RoutingContext ctx, String requestedCharset) {
        if (allowGet) {
            try {
                JsonObject input = getJsonObjectFromQueryParameters(ctx);

                if (input.containsKey(QUERY)) {
                    doRequest(input, response, ctx, requestedCharset);
                } else {
                    response.setStatusCode(400).end(MISSING_OPERATION);
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
        JsonObjectBuilder input = jsonProvider().createObjectBuilder();
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
            JsonObjectBuilder input = jsonProvider().createObjectBuilder();
            input.add(QUERY, body);
            return input.build();
            // Else we expect a Json in the content
        } else {
            if (body == null || body.isEmpty()) {
                return null;
            }
            try (StringReader bodyReader = new StringReader(body);
                    JsonReader jsonReader = jsonReaderFactory.createReader(bodyReader)) {
                return jsonReader.readObject();
            }
        }

    }

    private String readBody(RoutingContext ctx) {
        if (ctx.body() != null) {
            return ctx.body().asString();
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
        ParsedHeaderValues parsedHeaders = ctx.parsedHeaders();
        if (parsedHeaders != null && parsedHeaders.accept() != null && !parsedHeaders.accept().isEmpty()) {
            List<MIMEHeader> acceptList = parsedHeaders.accept();
            for (MIMEHeader a : acceptList) {
                if (isValidAcceptRequest(a.rawValue())) {
                    return a.rawValue();
                }

                if (a.rawValue().startsWith("*/*")) {
                    return DEFAULT_RESPONSE_CONTENT_TYPE;
                }
            }
            // Seems like an unknown accept is passed in
            String accept = ctx.request().getHeader("Accept");
            if (accept != null && !accept.isEmpty()) {
                return accept;
            }
        }
        return DEFAULT_RESPONSE_CONTENT_TYPE;
    }

    private static int indexOfSemicolonOrWhitespace(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == ';' || Character.isWhitespace(c)) {
                return i;
            }
        }
        return -1;
    }

    private static final String CHARSET_TOKEN = "charset=";
    private static final String UTF_8_LOW = "utf-8";
    private static final String UTF_8_HIGH = "UTF-8";

    private static String parseCharset(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        final int charsetIndex = mimeType.indexOf(CHARSET_TOKEN);
        if (charsetIndex == -1) {
            return null;
        }
        final int charsetValueStart = charsetIndex + CHARSET_TOKEN.length();
        final int firstSemicolonWhitespace = indexOfSemicolonOrWhitespace(mimeType, charsetValueStart);
        final int charsetValueEnd = firstSemicolonWhitespace == -1 ? mimeType.length() : firstSemicolonWhitespace;
        final int charsetLen = charsetValueEnd - charsetValueStart;
        if (charsetLen == 0) {
            return null;
        }
        return charsetOf(mimeType, charsetValueStart, charsetLen);
    }

    private static String charsetOf(String mimeType, int charsetValueStart, int charsetLen) {
        if (charsetLen == UTF_8_LOW.length()) {
            if (mimeType.regionMatches(charsetValueStart, UTF_8_LOW, 0, UTF_8_LOW.length())) {
                return UTF_8_LOW;
            }
            if (mimeType.regionMatches(charsetValueStart, UTF_8_HIGH, 0, UTF_8_LOW.length())) {
                return UTF_8_HIGH;
            }
        }
        return mimeType.substring(charsetValueStart, charsetValueStart + charsetLen);
    }

    private static String getCharset(String mimeType) {
        final String parsedCharset = parseCharset(mimeType);
        if (parsedCharset != null) {
            return parsedCharset;
        }
        return StandardCharsets.UTF_8.name();
    }

    private boolean isValidAcceptRequest(String mimeType) {
        return mimeType.startsWith("application/json")
                || mimeType.startsWith("application/graphql-response+json")
                // application/graphql+json is incorrect, but we keep it for backwards compatibility
                || mimeType.startsWith("application/graphql+json");
    }

    private String readQueryParameter(RoutingContext ctx, String parameterName) {
        List<String> all = ctx.queryParam(parameterName);
        if (all != null && !all.isEmpty()) {
            return all.get(0);
        }
        return null;
    }

    private static final Pattern PATTERN_NEWLINE_OR_TAB = Pattern.compile("\\n|\\t|\\r");

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
        return all != null && !all.isEmpty();
    }

    private String getAllowedMethods() {
        if (allowGet) {
            return "GET, POST, OPTIONS";
        } else {
            return "POST, OPTIONS";
        }
    }

    private void sendError(String errorMessage, HttpServerResponse response,
            RoutingContext ctx, String requestedCharset) {
        VertxExecutionResponseWriter writer = new VertxExecutionResponseWriter(response, ctx, requestedCharset);
        GraphQLError error = GraphqlErrorBuilder
                .newError()
                .message(errorMessage)
                .build();
        ExecutionResult executionResult = ExecutionResultImpl
                .newExecutionResult()
                .addError(error)
                .build();
        ExecutionResponse executionResponse = new ExecutionResponse(executionResult);
        writer.write(executionResponse);
    }

    private void doRequest(JsonObject jsonInput, HttpServerResponse response, RoutingContext ctx,
            String requestedCharset) {
        VertxExecutionResponseWriter writer = new VertxExecutionResponseWriter(response, ctx, requestedCharset);
        getExecutionService().executeAsync(jsonInput, getMetaData(ctx), writer);
    }

    private static JsonObject toJsonObject(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }

        try (JsonReader jsonReader = jsonReaderFactory.createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    class VertxExecutionResponseWriter implements ExecutionResponseWriter {

        HttpServerResponse response;
        String requestedCharset;
        RoutingContext ctx;

        VertxExecutionResponseWriter(HttpServerResponse response, RoutingContext ctx, String requestedCharset) {
            this.response = response;
            this.ctx = ctx;
            this.requestedCharset = requestedCharset;
        }

        @Override
        public void write(ExecutionResponse er) {
            if (shouldFail(er)) {
                er.getExecutionResult()
                        .getErrors().stream()
                        .filter(e -> e.getErrorType().equals(ErrorType.ExecutionAborted))
                        .forEach(e -> {
                            log.error("Execution aborted", (AbortExecutionException) e);
                        });
                response.setStatusCode(500)
                        .end();
            } else {
                try {
                    response.setStatusCode(200)
                            .setStatusMessage(OK)
                            .end(Buffer.buffer(er.getExecutionResultAsString(), requestedCharset));
                } catch (IllegalStateException ise) {
                    // The application already finished the request by itself for some reason
                    if (log.isDebugEnabled()) {
                        log.debug("Cannot write response", ise);
                    }
                }
            }
        }

        @Override
        public void fail(Throwable t) {
            ctx.fail(t);
        }

        private boolean shouldFail(ExecutionResponse er) {
            ExecutionResult executionResult = er.getExecutionResult();

            if (executionResult.isDataPresent() && executionResult.getErrors().size() > 0) {
                // See if there was a httpfailure
                for (GraphQLError error : executionResult.getErrors()) {
                    if (error.getErrorType().equals(ErrorType.ExecutionAborted)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
