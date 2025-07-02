package io.quarkus.devui.runtime.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

abstract class McpMessageHandler<MCP_REQUEST extends McpRequest> {

    private static final Logger LOG = Logger.getLogger(McpMessageHandler.class);

    protected final ConnectionManager connectionManager;
    protected final Vertx vertx;
    private final ToolMessageHandler toolHandler;
    private final ResponseHandlers responseHandlers;

    McpMessageHandler(ConnectionManager connectionManager, Vertx vertx, ToolMessageHandler toolHandler,
            ResponseHandlers responseHandlers) {
        this.connectionManager = connectionManager;
        this.vertx = vertx;
        this.toolHandler = toolHandler;
        this.responseHandlers = responseHandlers;
    }

    public Future<?> handle(MCP_REQUEST mcpRequest) {
        Object json = mcpRequest.json();
        if (json instanceof JsonObject message) {
            // Single request, notification, or response
            mcpRequest.messageReceived(message);
            if (JsonRPC.validate(message, mcpRequest.sender())) {
                return Messages.isResponse(message) ? handleResponse(message)
                        : handleRequest(message, mcpRequest);
            } else {
                jsonrpcValidationFailed(mcpRequest);
            }
        } else if (json instanceof JsonArray batch) {
            // Batch of messages
            if (!batch.isEmpty()) {
                List<Future<Void>> all = new ArrayList<>();
                if (Messages.isResponse(batch.getJsonObject(0))) {
                    // Batch of responses
                    for (Object e : batch) {
                        JsonObject response = (JsonObject) e;
                        mcpRequest.messageReceived(response);
                        if (JsonRPC.validate(response, mcpRequest.sender())) {
                            all.add(handleResponse(response));
                        } else {
                            jsonrpcValidationFailed(mcpRequest);
                        }
                    }
                } else {
                    // Batch of requests/notifications
                    for (Object e : batch) {
                        JsonObject requestOrNotification = (JsonObject) e;
                        mcpRequest.messageReceived(requestOrNotification);
                        if (JsonRPC.validate(requestOrNotification, mcpRequest.sender())) {
                            all.add(handleRequest(requestOrNotification, mcpRequest));
                        } else {
                            jsonrpcValidationFailed(mcpRequest);
                        }
                    }
                }
                return Future.all(all);
            }
        }
        return Future.failedFuture("Invalid jsonrpc message");
    }

    protected void jsonrpcValidationFailed(MCP_REQUEST mcpRequest) {
        // No-op
    }

    protected void initializeFailed(MCP_REQUEST mcpRequest) {
        // No-op
    }

    protected void afterInitialize(MCP_REQUEST mcpRequest) {
        // No-op
    }

    protected abstract InitialRequest.Transport transport();

    private Future<Void> handleResponse(JsonObject message) {
        return responseHandlers.handleResponse(message.getValue("id"), message);
    }

    private Future<Void> handleRequest(JsonObject message, MCP_REQUEST mcpRequest) {
        return switch (mcpRequest.connection().status()) {
            case NEW -> initializeNew(message, mcpRequest);
            case INITIALIZING -> initializing(message, mcpRequest);
            case IN_OPERATION -> operation(message, mcpRequest);
            case CLOSED -> mcpRequest.sender().send(
                    Messages.newError(message.getValue("id"), JsonRPC.INTERNAL_ERROR, "Connection is closed"));
        };
    }

    private Future<Void> initializeNew(JsonObject message, MCP_REQUEST mcpRequest) {
        Object id = message.getValue("id");
        String method = message.getString("method");
        JsonObject params = message.getJsonObject("params");

        // The first message must be "initialize"
        // However, in the dev mode if an MCP client attempts to reconnect an SSE connection but does not reinitialize propertly,
        // we could perform a "dummy" initialization
        if (!INITIALIZE.equals(method)) {
            boolean dummyInit = false; // TODO: is this needed?
            if (LaunchMode.current() == LaunchMode.DEVELOPMENT && dummyInit) {
                InitialRequest dummy = new InitialRequest(new Implementation("dummy", "1"), SUPPORTED_PROTOCOL_VERSIONS.get(0),
                        List.of(), transport());
                if (mcpRequest.connection().initialize(dummy) && mcpRequest.connection().setInitialized()) {
                    LOG.infof("Connection initialized with dummy info [%s]", mcpRequest.connection().id());
                    return operation(message, mcpRequest);
                }
            }

            String msg = "The first message from the client must be \"initialize\": " + method;
            initializeFailed(mcpRequest);
            return mcpRequest.sender().sendError(id, JsonRPC.METHOD_NOT_FOUND, msg);
        }

        if (params == null) {
            String msg = "Initialization params not found";
            initializeFailed(mcpRequest);
            return mcpRequest.sender().sendError(id, JsonRPC.INVALID_PARAMS, msg);
        }

        InitialRequest initialRequest = decodeInitializeRequest(params);
        // Start the context first
        mcpRequest.contextStart();
        if (mcpRequest.connection().initialize(initialRequest)) {
            // The server MUST respond with its own capabilities and information
            afterInitialize(mcpRequest);
            return mcpRequest.sender().sendResult(id, serverInfo(mcpRequest, initialRequest));
        } else {
            initializeFailed(mcpRequest);
            String msg = "Unable to initialize connection [connectionId: " + mcpRequest.connection().id() + "]";
            return mcpRequest.sender().sendError(id, JsonRPC.INTERNAL_ERROR, msg);
        }
    }

    private Future<Void> initializing(JsonObject message, McpRequest mcpRequest) {
        String method = message.getString("method");
        if (NOTIFICATIONS_INITIALIZED.equals(method)) {
            if (mcpRequest.connection().setInitialized()) {
                LOG.debugf("Client successfully initialized [%s]", mcpRequest.connection().id());
            }
            return Future.succeededFuture();
        } else if (PING.equals(method)) {
            return ping(message, mcpRequest);
        } else {
            return mcpRequest.sender().send(Messages.newError(message.getValue("id"), JsonRPC.INTERNAL_ERROR,
                    "Client not initialized yet [" + mcpRequest.connection().id() + "]"));
        }
    }

    public static final String INITIALIZE = "initialize";
    public static final String NOTIFICATIONS_INITIALIZED = "notifications/initialized";
    public static final String NOTIFICATIONS_MESSAGE = "notifications/message";
    public static final String NOTIFICATIONS_PROGRESS = "notifications/progress";
    public static final String NOTIFICATIONS_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";
    public static final String NOTIFICATIONS_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";
    public static final String NOTIFICATIONS_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";
    public static final String NOTIFICATIONS_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";
    public static final String PROMPTS_LIST = "prompts/list";
    public static final String PROMPTS_GET = "prompts/get";
    public static final String TOOLS_LIST = "tools/list";
    public static final String TOOLS_CALL = "tools/call";
    public static final String RESOURCES_LIST = "resources/list";
    public static final String RESOURCE_TEMPLATES_LIST = "resources/templates/list";
    public static final String RESOURCES_READ = "resources/read";
    public static final String RESOURCES_SUBSCRIBE = "resources/subscribe";
    public static final String RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";
    public static final String PING = "ping";
    public static final String ROOTS_LIST = "roots/list";
    public static final String SAMPLING_CREATE_MESSAGE = "sampling/createMessage";
    public static final String COMPLETION_COMPLETE = "completion/complete";
    public static final String LOGGING_SET_LEVEL = "logging/setLevel";
    // non-standard messages
    public static final String Q_CLOSE = "q/close";

    private Future<Void> operation(JsonObject message, McpRequest mcpRequest) {
        // Create a new duplicated context and process the operation on this context
        Context context = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        VertxContextSafetyToggle.setContextSafe(context, true);
        Promise<Void> ret = Promise.promise();

        context.runOnContext(v -> {
            mcpRequest.contextStart();
            String method = message.getString("method");
            Future<Void> future = switch (method) {
                //                case PROMPTS_LIST -> promptHandler.promptsList(message, mcpRequest);
                //                case PROMPTS_GET -> promptHandler.promptsGet(message, mcpRequest);
                case TOOLS_LIST -> toolHandler.toolsList(message, mcpRequest);
                case TOOLS_CALL -> toolHandler.toolsCall(message, mcpRequest);
                case PING -> ping(message, mcpRequest);
                //                case RESOURCES_LIST -> resourceHandler.resourcesList(message, mcpRequest);
                //                case RESOURCES_READ -> resourceHandler.resourcesRead(message, mcpRequest);
                //                case RESOURCES_SUBSCRIBE -> resourceHandler.resourcesSubscribe(message, mcpRequest);
                //                case RESOURCES_UNSUBSCRIBE -> resourceHandler.resourcesUnsubscribe(message, mcpRequest);
                //                case RESOURCE_TEMPLATES_LIST -> resourceTemplateHandler.resourceTemplatesList(message, mcpRequest);
                case COMPLETION_COMPLETE -> complete(message, mcpRequest);
                case LOGGING_SET_LEVEL -> setLogLevel(message, mcpRequest);
                case Q_CLOSE -> close(message, mcpRequest);
                default -> mcpRequest.sender().send(
                        Messages.newError(message.getValue("id"), JsonRPC.METHOD_NOT_FOUND, "Unsupported method: " + method));
            };
            future.onComplete(r -> {
                mcpRequest.contextEnd();
                if (r.failed()) {
                    ret.fail(r.cause());
                } else {
                    ret.complete();
                }
            });
        });
        return ret.future();
    }

    private Future<Void> setLogLevel(JsonObject message, McpRequest mcpRequest) {
        Object id = message.getValue("id");
        JsonObject params = message.getJsonObject("params");
        String level = params.getString("level");
        if (level == null) {
            return mcpRequest.sender().sendError(id, JsonRPC.INVALID_REQUEST, "Log level not set");
        } else {
            McpLog.LogLevel logLevel = McpLog.LogLevel.from(level);
            if (logLevel == null) {
                return mcpRequest.sender().sendError(id, JsonRPC.INVALID_REQUEST, "Invalid log level set: " + level);
            } else {
                mcpRequest.connection().setLogLevel(logLevel);
                // Send empty result
                return mcpRequest.sender().sendResult(id, new JsonObject());
            }
        }

    }

    private Future<Void> complete(JsonObject message, McpRequest mcpRequest) {
        Object id = message.getValue("id");
        JsonObject params = message.getJsonObject("params");
        JsonObject ref = params.getJsonObject("ref");
        if (ref == null) {
            return mcpRequest.sender().sendError(id, JsonRPC.INVALID_REQUEST, "Reference not found");
        } else {
            String referenceType = ref.getString("type");
            if (referenceType == null) {
                return mcpRequest.sender().sendError(id, JsonRPC.INVALID_REQUEST, "Reference type not found");
            } else {
                JsonObject argument = params.getJsonObject("argument");
                if (argument == null) {
                    return mcpRequest.sender().sendError(id, JsonRPC.INVALID_REQUEST, "Argument not found");
                } else {
                    //                    if ("ref/prompt".equals(referenceType)) {
                    //                        return promptCompleteHandler.complete(message, id, ref, argument, mcpRequest.sender(), mcpRequest);
                    //                    } else if ("ref/resource".equals(referenceType)) {
                    //                        return resourceTemplateCompleteHandler.complete(message, id, ref, argument, mcpRequest.sender(),
                    //                                mcpRequest);
                    //                    } else {
                    //                        return mcpRequest.sender().sendError(id, JsonRPC.INVALID_REQUEST,
                    //                                "Unsupported reference found: " + ref.getString("type"));
                    //                    }

                    return mcpRequest.sender().sendError(id, JsonRPC.INVALID_REQUEST,
                            "Unsupported reference found: " + ref.getString("type"));
                }
            }
        }
    }

    private Future<Void> ping(JsonObject message, McpRequest mcpRequest) {
        Object id = message.getValue("id");
        LOG.debugf("Ping [id: %s]", id);
        return mcpRequest.sender().sendResult(id, new JsonObject());
    }

    private Future<Void> close(JsonObject message, McpRequest mcpRequest) {
        if (connectionManager.remove(mcpRequest.connection().id())) {
            LOG.debugf("Connection %s explicitly closed ", mcpRequest.connection().id());
            return Future.succeededFuture();
        } else {
            return mcpRequest.sender().sendError(message.getValue("id"), JsonRPC.INTERNAL_ERROR,
                    "Unable to obtain the connection to be closed:" + mcpRequest.connection().id());
        }
    }

    private InitialRequest decodeInitializeRequest(JsonObject params) {
        JsonObject clientInfo = params.getJsonObject("clientInfo");
        Implementation implementation = new Implementation(clientInfo.getString("name"), clientInfo.getString("version"));
        String protocolVersion = params.getString("protocolVersion");
        List<ClientCapability> clientCapabilities = new ArrayList<>();
        JsonObject capabilities = params.getJsonObject("capabilities");
        if (capabilities != null) {
            for (String name : capabilities.fieldNames()) {
                // TODO capability properties
                clientCapabilities.add(new ClientCapability(name, Map.of()));
            }
        }
        return new InitialRequest(implementation, protocolVersion, List.copyOf(clientCapabilities), transport());
    }

    private static final List<String> SUPPORTED_PROTOCOL_VERSIONS = List.of("2025-03-26", "2024-11-05");

    private Map<String, Object> serverInfo(MCP_REQUEST mcpRequest, InitialRequest initialRequest) {
        Map<String, Object> info = new HashMap<>();

        // Note that currently the protocol version does not affect the behavior of the server in any way
        String version = SUPPORTED_PROTOCOL_VERSIONS.get(0);
        if (SUPPORTED_PROTOCOL_VERSIONS.contains(initialRequest.protocolVersion())) {
            version = initialRequest.protocolVersion();
        }
        info.put("protocolVersion", version);

        Config config = ConfigProvider.getConfig();
        String serverName = config.getOptionalValue("quarkus.application.name", String.class)
                .orElse("N/A");
        String serverVersion = config.getOptionalValue("quarkus.application.version", String.class)
                .orElse("N/A");
        info.put("serverInfo", Map.of("name", serverName, "version", serverVersion));

        Map<String, Map<String, Object>> capabilities = new HashMap<>();
        //        if (promptManager.hasInfos(mcpRequest)) {
        //            capabilities.put("prompts", metadata.isPromptManagerUsed() ? Map.of("listChanged", true) : Map.of());
        //        }
        capabilities.put("tools", Map.of("listChanged", true));
        //        if (resourceManager.hasInfos(mcpRequest)
        //            || resourceTemplateManager.hasInfos(mcpRequest)) {
        //            capabilities.put("resources", metadata.isResourceManagerUsed() ? Map.of("listChanged", true) : Map.of());
        //        }
        //        if (promptCompletionManager.hasInfos(mcpRequest)
        //            || resourceTemplateCompletionManager.hasInfos(mcpRequest)) {
        //            capabilities.put("completions", Map.of());
        //        }
        capabilities.put("logging", Map.of());
        info.put("capabilities", capabilities);
        return info;
    }

}
