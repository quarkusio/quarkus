package io.quarkus.devui.runtime.mcp;

import static io.quarkus.devui.runtime.mcp.FeatureArgument.Provider.MCP_LOG;
import static io.quarkus.devui.runtime.mcp.FeatureArgument.Provider.PROGRESS;
import static io.quarkus.devui.runtime.mcp.FeatureArgument.Provider.ROOTS;
import static io.quarkus.devui.runtime.mcp.FeatureArgument.Provider.SAMPLING;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Alternative Dev UI Json RPC communication using MCP's Streamable HTTP protocol
 */
public class StreamableHttpHandler extends McpMessageHandler<StreamableHttpHandler.HttpMcpRequest>
        implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(StreamableHttpHandler.class.getName());

    private static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";

    private final McpServerRuntimeConfig config;

    public StreamableHttpHandler(Vertx vertx, ToolMessageHandler toolHandler,
            ResponseHandlers responseHandlers, McpServerRuntimeConfig config) {
        super(new ConnectionManager(vertx, responseHandlers, config), vertx, toolHandler, responseHandlers);
        this.config = config;
    }

    @Override
    public void handle(RoutingContext ctx) {
        HttpMethod httpMethod = ctx.request().method();
        LOG.debug(httpMethod.name() + " : " + ctx.request().absoluteURI());

        if (httpMethod.equals(HttpMethod.GET)) {
            ctx.response().setStatusCode(405).end();
        } else if (HttpMethod.POST.equals(httpMethod)) {
            doHandle(ctx);
        } else if (HttpMethod.DELETE.equals(httpMethod)) {
            terminateSession(ctx);
        } else {
            throw new IllegalArgumentException("Unexpected HTTP method: " + httpMethod);
        }
    }

    public void terminateSession(RoutingContext ctx) {
        HttpServerRequest request = ctx.request();
        String mcpSessionId = request.getHeader(MCP_SESSION_ID_HEADER);
        if (mcpSessionId == null) {
            LOG.errorf("Mcp session id header is missing: %s", ctx.normalizedPath());
            ctx.fail(404);
            return;
        }
        McpConnectionBase connection = connectionManager.get(mcpSessionId);
        if (connection == null) {
            LOG.errorf("Mcp session not found: %s", mcpSessionId);
            ctx.fail(404);
            return;
        }
        if (connectionManager.remove(connection.id())) {
            LOG.debugf("Mcp session terminated: %s", connection.id());
        }
        ctx.end();
    }

    @Override
    protected void afterInitialize(HttpMcpRequest mcpRequest) {
        // Add the "Mcp-Session-Id" header to the response to the "Initialize" request
        mcpRequest.response.headers().add(MCP_SESSION_ID_HEADER, mcpRequest.connection().id());
    }

    @Override
    protected void initializeFailed(HttpMcpRequest mcpRequest) {
        connectionManager.remove(mcpRequest.connection().id());
    }

    @Override
    protected void jsonrpcValidationFailed(HttpMcpRequest mcpRequest) {
        if (mcpRequest.newSession) {
            connectionManager.remove(mcpRequest.connection().id());
        }
    }

    private void doHandle(RoutingContext ctx) {
        //        String serverName = ctx.get(SseMcpServerRecorder.CONTEXT_KEY);
        //        if (serverName == null) {
        //            throw new IllegalStateException("Server name not defined");
        //        }
        String serverName = "Quarkus DevUI"; // TODO: do we need this?
        HttpServerRequest request = ctx.request();

        // The client MUST include an "Accept" header,
        // listing both "application/json" and "text/event-stream" as supported content types
        List<String> accepts = ctx.request().headers().getAll(HttpHeaders.ACCEPT);
        if (!accepts(accepts, "application/json")
                || !accepts(accepts, "text/event-stream")) {
            LOG.errorf("Invalid Accept header: %s", accepts);
            ctx.fail(400);
            return;
        }

        McpConnectionBase connection;
        String mcpSessionId = request.getHeader(MCP_SESSION_ID_HEADER);
        if (mcpSessionId == null) {
            String id = ConnectionManager.connectionId();
            LOG.debugf("Streamable connection initialized [%s]", id);
            connection = new StreamableHttpMcpConnection(id, config);
            connectionManager.add(connection);
        } else {
            connection = connectionManager.get(mcpSessionId);
        }

        if (connection == null) {
            LOG.errorf("Mcp session not found: %s", mcpSessionId);
            ctx.fail(404);
            return;
        }

        Object json;
        try {
            json = Json.decodeValue(ctx.body().buffer());
        } catch (Exception e) {
            String msg = "Unable to parse the JSON message";
            LOG.errorf(e, msg);
            ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            ctx.end(Messages.newError(null, JsonRPC.PARSE_ERROR, msg).toBuffer());
            return;
        }
        ContextSupport contextSupport = new ContextSupport() {
            @Override
            public void requestContextActivated() {
                // TODO: is this really needed?
            }
        };

        HttpMcpRequest mcpRequest = new HttpMcpRequest(serverName, json, connection, ctx.response(),
                mcpSessionId == null, contextSupport);
        ScanResult result = scan(mcpRequest);
        if (result.forceSseInit()) {
            mcpRequest.initiateSse();
        }
        handle(mcpRequest).onComplete(ar -> {
            if (ar.succeeded()) {
                if (mcpRequest.sse.get()) {
                    // Just close the SSE stream
                    ctx.response().end();
                } else {
                    if (!ctx.response().ended()) {
                        if (!result.containsRequest()) {
                            // If the input consists solely of responses/notifications
                            // then the server MUST return HTTP status 202
                            ctx.response().setStatusCode(202).end();
                        } else {
                            ctx.end();
                        }
                    }
                }
            } else {
                if (!ctx.response().ended()) {
                    ctx.response().setStatusCode(500).end();
                }
            }
        });
    }

    private boolean accepts(List<String> accepts, String contentType) {
        for (String accept : accepts) {
            if (accept.contains(contentType)) {
                return true;
            }
        }
        return false;
    }

    private static final Set<String> FORCE_SSE_REQUESTS = Set.of(
            TOOLS_CALL,
            PROMPTS_GET,
            RESOURCES_READ,
            COMPLETION_COMPLETE);

    private static final Set<String> FORCE_SSE_NOTIFICATIONS = Set.of(
            NOTIFICATIONS_INITIALIZED,
            NOTIFICATIONS_ROOTS_LIST_CHANGED);

    private static final Set<FeatureArgument.Provider> FORCE_SSE_PROVIDERS = Set.of(
            PROGRESS,
            MCP_LOG,
            SAMPLING,
            ROOTS);

    record ScanResult(boolean forceSseInit, boolean containsRequest) {
    }

    private ScanResult scan(HttpMcpRequest mcpRequest) {
        boolean forceSseInit = false;
        boolean containsRequest = false;
        // Scan the request payload and attempt to identify messages that should force SSE init
        // such as a tool call with the Progress param
        if (mcpRequest.json() instanceof JsonObject message) {
            forceSseInit = forceSse(message);
            containsRequest = Messages.isRequest(message);
        } else if (mcpRequest.json() instanceof JsonArray batch) {
            if (!Messages.isResponse(batch.getJsonObject(0))) {
                // The batch contains at least 2 requests/notifications
                // or 1 requests/notification that forces SSE init
                forceSseInit = batch.size() > 1 || forceSse(batch.getJsonObject(0));
                for (Object e : batch) {
                    if (e instanceof JsonObject message && Messages.isRequest(message)) {
                        containsRequest = true;
                        break;
                    }
                }
            }
        }
        return new ScanResult(forceSseInit, containsRequest);
    }

    private boolean forceSse(JsonObject message) {
        String method = message.getString("method");
        if (method != null) {
            if (Messages.isRequest(message) && FORCE_SSE_REQUESTS.contains(method)) {
                JsonObject params = message.getJsonObject("params");
                if (params != null) {
                    return switch (method) {
                        case TOOLS_CALL -> forceSseTool(params);
                        case PROMPTS_GET -> forceSsePrompt(params);
                        case RESOURCES_READ -> forceSseResource(params);
                        case COMPLETION_COMPLETE -> forceSseCompletion(params);
                        default -> throw new IllegalArgumentException("Unexpected value: " + method);
                    };
                }
            } else if (Messages.isNotification(message)
                    && FORCE_SSE_NOTIFICATIONS.contains(method)
                    && forceSseNotification(method)) {
                return true;
            }
        }
        return false;
    }

    private boolean forceSseTool(JsonObject params) {
        //        String toolName = params.getString("name");
        //        FeatureMetadata<?> fm = metadata.tools().stream().filter(m -> m.info().name().equals(toolName))
        //                .findFirst().orElse(null);
        //        if (fm != null) {
        //            for (FeatureArgument a : fm.info().arguments()) {
        //                if (FORCE_SSE_PROVIDERS.contains(a.provider())) {
        //                    return true;
        //                }
        //            }
        //        } else {
        //            ToolInfo info = toolManager.getTool(toolName);
        //            if (info != null && !info.isMethod()) {
        //                // Always force SSE init for a tool added programatically
        //                return true;
        //            }
        //        }
        // TODO: is this correct?
        return false;
    }

    private boolean forceSsePrompt(JsonObject params) {
        //        String promptName = params.getString("name");
        //        FeatureMetadata<?> fm = metadata.prompts().stream().filter(m -> m.info().name().equals(promptName))
        //                .findFirst().orElse(null);
        //        if (fm != null) {
        //            for (FeatureArgument a : fm.info().arguments()) {
        //                if (FORCE_SSE_PROVIDERS.contains(a.provider())) {
        //                    return true;
        //                }
        //            }
        //        } else {
        //            PromptInfo info = promptManager.getPrompt(promptName);
        //            if (info != null && !info.isMethod()) {
        //                // Always force SSE init for a prompt added programatically
        //                return true;
        //            }
        //        }
        // TODO: is this correct?
        return false;
    }

    private boolean forceSseResource(JsonObject params) {
        //        String resourceUri = params.getString("uri");
        //        FeatureMetadata<?> fm = metadata.resources().stream().filter(m -> m.info().uri().equals(resourceUri))
        //                .findFirst().orElse(null);
        //        if (fm != null) {
        //            for (FeatureArgument a : fm.info().arguments()) {
        //                if (FORCE_SSE_PROVIDERS.contains(a.provider())) {
        //                    return true;
        //                }
        //            }
        //        } else {
        //            ResourceManager.ResourceInfo info = resourceManager.getResource(resourceUri);
        //            if (info != null) {
        //                if (!info.isMethod()) {
        //                    // Always force SSE init for a resource added programatically
        //                    return true;
        //                }
        //            } else {
        //                // Also try resource templates
        //                ResourceTemplateManager.ResourceTemplateInfo rti = resourceTemplateManager.findMatching(resourceUri);
        //                if (rti != null && !rti.isMethod()) {
        //                    return true;
        //                }
        //            }
        //        }
        // TODO: is this correct?
        return false;
    }

    private boolean forceSseCompletion(JsonObject params) {
        //        JsonObject ref = params.getJsonObject("ref");
        //        if (ref != null) {
        //            String referenceType = ref.getString("type");
        //            String referenceName = ref.getString("name");
        //            JsonObject argument = params.getJsonObject("argument");
        //            String argumentName = argument != null ? argument.getString("name") : null;
        //            if (referenceName != null && argumentName != null) {
        //                if ("ref/prompt".equals(referenceType)) {
        //                    return forceSseCompletion(referenceName, argumentName, metadata.promptCompletions(),
        //                            promptCompletionManager);
        //                } else if ("ref/resource".equals(referenceType)) {
        //                    return forceSseCompletion(referenceName, argumentName, metadata.resourceTemplateCompletions(),
        //                            resourceTemplateCompletionManager);
        //                }
        //            }
        //        }
        // TODO: is this correct?
        return false;

    }

    //    private boolean forceSseCompletion(String referenceName, String argumentName,
    //                                       List<FeatureMetadata<CompletionResponse>> completions, CompletionManager completionManager) {
    //        FeatureMetadata<?> fm = completions.stream().filter(m -> {
    //                    return m.info().name().equals(referenceName)
    //                           && argumentName.equals(m.info().arguments().stream().filter(FeatureArgument::isParam).findFirst()
    //                            .orElseThrow().name());
    //                })
    //                .findFirst().orElse(null);
    //        if (fm != null) {
    //            for (FeatureArgument a : fm.info().arguments()) {
    //                if (FORCE_SSE_PROVIDERS.contains(a.provider())) {
    //                    return true;
    //                }
    //            }
    //        } else {
    //            CompletionManager.CompletionInfo info = completionManager.getCompletion(referenceName, argumentName);
    //            if (info != null && !info.isMethod()) {
    //                // Always force SSE init for a completion added programatically
    //                return true;
    //            }
    //        }
    //        return false;
    //    }

    private boolean forceSseNotification(String method) {
        //        List<FeatureMetadata<Void>> fm = metadata.notifications()
        //                .stream()
        //                .filter(m -> Notification.Type.valueOf(m.info().description()) == Notification.Type.from(method))
        //                .toList();
        //        for (FeatureMetadata<?> m : fm) {
        //            for (FeatureArgument a : m.info().arguments()) {
        //                if (FORCE_SSE_PROVIDERS.contains(a.provider())) {
        //                    return true;
        //                }
        //            }
        //        }
        //        for (NotificationManager.NotificationInfo info : notificationManager) {
        //            if (!info.isMethod() && info.type() == Type.from(method)) {
        //                // Always force SSE init for a notification added programatically
        //                return true;
        //            }
        //        }
        // TODO: is this correct?
        return false;
    }

    @Override
    protected InitialRequest.Transport transport() {
        return InitialRequest.Transport.STREAMABLE_HTTP;
    }

    static class HttpMcpRequest extends McpRequestImpl implements Sender {

        final boolean newSession;

        final AtomicBoolean sse;

        final HttpServerResponse response;

        public HttpMcpRequest(String serverName, Object json, McpConnectionBase connection,
                HttpServerResponse response, boolean newSession, ContextSupport contextSupport) {
            super(serverName, json, connection, null, contextSupport);
            this.newSession = newSession;
            this.sse = new AtomicBoolean(false);
            this.response = response;
        }

        @Override
        public Sender sender() {
            return this;
        }

        boolean initiateSse() {
            if (sse.compareAndSet(false, true)) {
                response.setChunked(true);
                response.headers().add(HttpHeaders.CONTENT_TYPE, "text/event-stream");
                return true;
            }
            return false;
        }

        @Override
        public Future<Void> send(JsonObject message) {
            if (message == null) {
                return Future.succeededFuture();
            }
            messageSent(message);
            if (sse.get()) {
                // "write" is async and synchronized over http connection, and should be thread-safe
                return response.write("event: message\ndata: " + message.encode() + "\n\n");
            } else {
                response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                return response.end(message.toBuffer());
            }
        }

    }
}
