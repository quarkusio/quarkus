package io.quarkus.devtools.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Standalone MCP stdio server for managing Quarkus application lifecycle.
 * <p>
 * This server runs as a separate process, communicating via JSON-RPC 2.0
 * over stdin/stdout. It manages Quarkus dev mode instances as child processes,
 * surviving app crashes so that AI coding agents can recover broken applications.
 */
public class McpLifecycleServer {

    private static final String JSONRPC_VERSION = "2.0";
    private static final String MCP_PROTOCOL_VERSION = "2025-03-26";
    private static final String SERVER_NAME = "quarkus-lifecycle";
    private static final String SERVER_VERSION = "1.0.0";

    private final ObjectMapper mapper = new ObjectMapper();
    private final QuarkusProcessManager processManager = new QuarkusProcessManager();
    private final DocSearchService docSearchService = new DocSearchService();
    private final PrintStream out;

    public McpLifecycleServer(PrintStream out) {
        this.out = out;
    }

    public static void main(String[] args) throws IOException {
        // Use stdout for MCP JSON-RPC, redirect app logs to stderr
        PrintStream mcpOut = System.out;
        System.setOut(System.err);

        McpLifecycleServer server = new McpLifecycleServer(mcpOut);

        // Shut down all managed child processes when the MCP server exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("[mcp] Shutting down — stopping all managed Quarkus instances...");
            server.processManager.stopAll();
        }));

        server.run();
    }

    public void run() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                JsonNode request = mapper.readTree(line);
                JsonNode response = handleRequest(request);
                if (response != null) {
                    sendResponse(response);
                }
            } catch (JsonProcessingException e) {
                sendResponse(errorResponse(null, -32700, "Parse error: " + e.getMessage()));
            }
        }
    }

    private JsonNode handleRequest(JsonNode request) {
        JsonNode idNode = request.get("id");
        String method = request.has("method") ? request.get("method").asText() : null;
        JsonNode params = request.get("params");

        if (method == null) {
            return errorResponse(idNode, -32600, "Invalid request: missing method");
        }

        // Notifications (no id) — don't send a response
        if (idNode == null) {
            handleNotification(method, params);
            return null;
        }

        return switch (method) {
            case "initialize" -> handleInitialize(idNode);
            case "ping" -> successResponse(idNode, mapper.createObjectNode());
            case "tools/list" -> handleToolsList(idNode);
            case "tools/call" -> handleToolsCall(idNode, params);
            default -> errorResponse(idNode, -32601, "Method not found: " + method);
        };
    }

    private void handleNotification(String method, JsonNode params) {
        // notifications like "initialized" or "notifications/cancelled" — no response needed
        if ("initialized".equals(method)) {
            System.err.println("[mcp] Client initialized");
        }
    }

    private JsonNode handleInitialize(JsonNode id) {
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", MCP_PROTOCOL_VERSION);

        ObjectNode capabilities = mapper.createObjectNode();
        capabilities.set("tools", mapper.createObjectNode());
        result.set("capabilities", capabilities);

        ObjectNode serverInfo = mapper.createObjectNode();
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("version", SERVER_VERSION);
        result.set("serverInfo", serverInfo);

        return successResponse(id, result);
    }

    private JsonNode handleToolsList(JsonNode id) {
        List<Map<String, Object>> tools = List.of(
                toolDef("quarkus/start", "Start a Quarkus application in dev mode",
                        Map.of(
                                "projectDir", propDef("string",
                                        "Absolute path to the Quarkus project directory", true),
                                "buildTool", propDef("string",
                                        "Build tool to use: 'maven' or 'gradle' (auto-detected if omitted)", false))),
                toolDef("quarkus/stop", "Stop a running Quarkus application",
                        Map.of(
                                "projectDir", propDef("string",
                                        "Absolute path to the Quarkus project directory", true))),
                toolDef("quarkus/restart", "Force restart a Quarkus application (sends 's' to dev mode console)",
                        Map.of(
                                "projectDir", propDef("string",
                                        "Absolute path to the Quarkus project directory", true))),
                toolDef("quarkus/status",
                        "Get the status of a Quarkus application (not_started, starting, running, crashed, stopped)",
                        Map.of(
                                "projectDir", propDef("string",
                                        "Absolute path to the Quarkus project directory", true))),
                toolDef("quarkus/logs", "Get recent log output from a Quarkus application",
                        Map.of(
                                "projectDir", propDef("string",
                                        "Absolute path to the Quarkus project directory", true),
                                "lines", propDef("integer",
                                        "Number of recent lines to return (default: 50)", false))),
                toolDef("quarkus/list", "List all managed Quarkus application instances and their status",
                        Map.of()),
                toolDef("quarkus/searchTools",
                        "Search available tools on a running Quarkus application's Dev MCP server. "
                                + "Use this to discover what actions you can perform on the running app. "
                                + "Available tools typically include: continuous testing (start/stop/run tests), "
                                + "configuration management, extension add/remove, log level control, "
                                + "dev services info, workspace file operations, and endpoint listing. "
                                + "IMPORTANT: When the user asks to do something with a running Quarkus app "
                                + "(e.g. 'run tests', 'start testing', 'change config', 'add extension'), "
                                + "call this tool first to discover the tool name, then use quarkus/callTool to invoke it.",
                        Map.of(
                                "projectDir", propDef("string",
                                        "Absolute path to the Quarkus project directory", true),
                                "query", propDef("string",
                                        "Search query to filter tools by name or description (case-insensitive). "
                                                + "Examples: 'test' for testing tools, 'config' for configuration, "
                                                + "'extension' for extension management. If omitted, returns all tools.",
                                        false))),
                toolDef("quarkus/callTool",
                        "Call a tool on the running Quarkus application's Dev MCP server. "
                                + "Use quarkus/searchTools first to discover available tool names and their required arguments, "
                                + "then use this tool to invoke them. "
                                + "Example: toolName='devui-continuous-testing_start' to start continuous testing.",
                        Map.of(
                                "projectDir", propDef("string",
                                        "Absolute path to the Quarkus project directory", true),
                                "toolName", propDef("string",
                                        "The name of the Dev MCP tool to call (as returned by quarkus/searchTools)", true),
                                "toolArguments", propDef("object",
                                        "Arguments to pass to the tool (as a JSON object matching the tool's inputSchema). "
                                                + "Omit if the tool takes no arguments.",
                                        false))),
                toolDef("quarkus/searchDocs",
                        "Search the Quarkus documentation using semantic search. "
                                + "Returns relevant documentation chunks matching the query. "
                                + "Use this when the user needs help with Quarkus APIs, configuration, extensions, "
                                + "or any Quarkus-related question. "
                                + "The first call may take a moment to start the documentation database.",
                        Map.of(
                                "query", propDef("string",
                                        "The search query describing what documentation you're looking for. "
                                                + "Examples: 'how to configure datasource', 'CDI dependency injection', "
                                                + "'REST client configuration', 'native image build'.",
                                        true),
                                "maxResults", propDef("integer",
                                        "Maximum number of documentation chunks to return (default: 4).",
                                        false))));

        return successResponse(id, mapper.valueToTree(Map.of("tools", tools)));
    }

    private JsonNode handleToolsCall(JsonNode id, JsonNode params) {
        if (params == null || !params.has("name")) {
            return errorResponse(id, -32602, "Invalid params: missing tool name");
        }

        String toolName = params.get("name").asText();
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : mapper.createObjectNode();

        try {
            String result = switch (toolName) {
                case "quarkus/start" -> toolStart(arguments);
                case "quarkus/stop" -> toolStop(arguments);
                case "quarkus/restart" -> toolRestart(arguments);
                case "quarkus/status" -> toolStatus(arguments);
                case "quarkus/logs" -> toolLogs(arguments);
                case "quarkus/list" -> toolList();
                case "quarkus/searchTools" -> toolSearchTools(arguments);
                case "quarkus/callTool" -> toolCallTool(arguments);
                case "quarkus/searchDocs" -> toolSearchDocs(arguments);
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };

            return successResponse(id, callToolResult(result, false));
        } catch (Exception e) {
            return successResponse(id, callToolResult(e.getMessage(), true));
        }
    }

    // --- Tool implementations ---

    private String toolStart(JsonNode args) {
        String projectDir = requireString(args, "projectDir");
        String buildTool = optString(args, "buildTool", null);
        processManager.start(projectDir, buildTool);
        return "Quarkus application starting in dev mode at: " + projectDir;
    }

    private String toolStop(JsonNode args) {
        String projectDir = requireString(args, "projectDir");
        processManager.stop(projectDir);
        return "Quarkus application stopped at: " + projectDir;
    }

    private String toolRestart(JsonNode args) {
        String projectDir = requireString(args, "projectDir");
        processManager.restart(projectDir);
        return "Quarkus application restart triggered at: " + projectDir;
    }

    private String toolStatus(JsonNode args) {
        String projectDir = requireString(args, "projectDir");
        QuarkusInstance instance = processManager.getInstance(projectDir);
        if (instance == null) {
            return "not_started";
        }
        String status = instance.getStatus().name().toLowerCase();
        if (instance.getStatus() == QuarkusInstance.Status.RUNNING && instance.getHttpPort() > 0) {
            return status + " (port: " + instance.getHttpPort()
                    + "). Use quarkus/searchTools to discover available Dev MCP tools "
                    + "(testing, config, extensions, etc.). "
                    + "Use quarkus/searchDocs to search the Quarkus documentation for any Quarkus-related questions.";
        }
        return status + ". Use quarkus/searchDocs to search the Quarkus documentation for any Quarkus-related questions.";
    }

    private String toolLogs(JsonNode args) {
        String projectDir = requireString(args, "projectDir");
        int lines = optInt(args, "lines", 50);
        QuarkusInstance instance = processManager.getInstance(projectDir);
        if (instance == null) {
            return "No instance found for: " + projectDir;
        }
        return instance.getRecentLogs(lines);
    }

    private String toolList() throws JsonProcessingException {
        Map<String, String> instances = processManager.listInstances();
        if (instances.isEmpty()) {
            return "No managed Quarkus instances";
        }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(instances);
    }

    private String toolSearchDocs(JsonNode args) {
        String query = requireString(args, "query");
        int maxResults = optInt(args, "maxResults", 4);

        List<Map<String, Object>> results = docSearchService.search(query, maxResults);
        if (results.isEmpty()) {
            return "No documentation found matching: " + query;
        }

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
        } catch (JsonProcessingException e) {
            return results.toString();
        }
    }

    private String toolSearchTools(JsonNode args) {
        String projectDir = requireString(args, "projectDir");
        String query = optString(args, "query", null);

        QuarkusInstance instance = processManager.getInstance(projectDir);
        if (instance == null || instance.getStatus() != QuarkusInstance.Status.RUNNING) {
            throw new IllegalStateException(
                    "Quarkus application is not running at: " + projectDir + ". Start it first with quarkus/start.");
        }

        int port = instance.getHttpPort();
        if (port < 0) {
            throw new IllegalStateException("Could not detect HTTP port for the running Quarkus application.");
        }

        // Query Dev MCP for tools/list
        JsonNode tools = fetchDevMcpTools(port);
        if (tools == null || !tools.isArray()) {
            return "No tools available from Dev MCP";
        }

        // Filter by query if provided
        List<JsonNode> matched = new ArrayList<>();
        for (JsonNode tool : tools) {
            if (query == null || query.isEmpty()) {
                matched.add(tool);
            } else {
                String name = tool.has("name") ? tool.get("name").asText().toLowerCase() : "";
                String desc = tool.has("description") ? tool.get("description").asText().toLowerCase() : "";
                if (name.contains(query.toLowerCase()) || desc.contains(query.toLowerCase())) {
                    matched.add(tool);
                }
            }
        }

        if (matched.isEmpty()) {
            return "No Dev MCP tools found matching: " + query;
        }

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(matched);
        } catch (JsonProcessingException e) {
            return matched.toString();
        }
    }

    private String toolCallTool(JsonNode args) {
        String projectDir = requireString(args, "projectDir");
        String toolName = requireString(args, "toolName");
        JsonNode toolArguments = args.has("toolArguments") && !args.get("toolArguments").isNull()
                ? args.get("toolArguments")
                : mapper.createObjectNode();

        QuarkusInstance instance = processManager.getInstance(projectDir);
        if (instance == null || instance.getStatus() != QuarkusInstance.Status.RUNNING) {
            throw new IllegalStateException(
                    "Quarkus application is not running at: " + projectDir + ". Start it first with quarkus/start.");
        }

        int port = instance.getHttpPort();
        if (port < 0) {
            throw new IllegalStateException("Could not detect HTTP port for the running Quarkus application.");
        }

        // Call the Dev MCP tool via tools/call
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", toolName);
        params.put("arguments", mapper.convertValue(toolArguments, Map.class));

        JsonNode response = callDevMcp(port, "tools/call", params);

        // Extract the result text
        if (response != null && response.has("content") && response.get("content").isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode content : response.get("content")) {
                if (content.has("text")) {
                    if (!sb.isEmpty()) {
                        sb.append("\n");
                    }
                    sb.append(content.get("text").asText());
                }
            }
            if (response.has("isError") && response.get("isError").asBoolean()) {
                throw new RuntimeException("Dev MCP tool error: " + sb);
            }
            return sb.toString();
        }

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return response != null ? response.toString() : "No response from Dev MCP";
        }
    }

    // --- Dev MCP HTTP client ---

    private JsonNode fetchDevMcpTools(int port) {
        JsonNode result = callDevMcp(port, "tools/list", Map.of());
        if (result != null && result.has("tools")) {
            return result.get("tools");
        }
        return null;
    }

    private JsonNode callDevMcp(int port, String method, Map<String, Object> params) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            String jsonRpcRequest = mapper.writeValueAsString(Map.of(
                    "jsonrpc", "2.0",
                    "id", 1,
                    "method", method,
                    "params", params));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/q/dev-mcp"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRpcRequest))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Dev MCP returned HTTP " + response.statusCode());
            }

            JsonNode body = mapper.readTree(response.body());
            if (body.has("result")) {
                return body.get("result");
            }
            if (body.has("error")) {
                String errorMsg = body.get("error").has("message")
                        ? body.get("error").get("message").asText()
                        : body.get("error").toString();
                throw new RuntimeException("Dev MCP error: " + errorMsg);
            }
            return null;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to call Dev MCP: " + e.getMessage(), e);
        }
    }

    // --- JSON-RPC helpers ---

    private JsonNode successResponse(JsonNode id, JsonNode result) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    private JsonNode errorResponse(JsonNode id, int code, String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.set("id", id != null ? id : mapper.nullNode());
        ObjectNode error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        response.set("error", error);
        return response;
    }

    private JsonNode callToolResult(String text, boolean isError) {
        ObjectNode result = mapper.createObjectNode();
        ObjectNode content = mapper.createObjectNode();
        content.put("type", "text");
        content.put("text", text);
        result.set("content", mapper.createArrayNode().add(content));
        result.put("isError", isError);
        return result;
    }

    private void sendResponse(JsonNode response) {
        try {
            String json = mapper.writeValueAsString(response);
            out.println(json);
            out.flush();
        } catch (JsonProcessingException e) {
            System.err.println("[mcp] Failed to serialize response: " + e.getMessage());
        }
    }

    // --- Tool definition helpers ---

    private Map<String, Object> toolDef(String name, String description, Map<String, Object> properties) {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", name);
        tool.put("description", description);

        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);

        List<String> required = properties.entrySet().stream()
                .filter(e -> {
                    Object val = e.getValue();
                    return val instanceof Map && Boolean.TRUE.equals(((Map<?, ?>) val).get("required"));
                })
                .map(Map.Entry::getKey)
                .toList();

        // Remove the "required" field from each property (it's not part of JSON Schema property defs)
        Map<String, Object> cleanedProps = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> prop = new LinkedHashMap<>((Map<String, Object>) entry.getValue());
                prop.remove("required");
                cleanedProps.put(entry.getKey(), prop);
            } else {
                cleanedProps.put(entry.getKey(), entry.getValue());
            }
        }
        inputSchema.put("properties", cleanedProps);

        if (!required.isEmpty()) {
            inputSchema.put("required", required);
        }

        tool.put("inputSchema", inputSchema);
        return tool;
    }

    private Map<String, Object> propDef(String type, String description, boolean required) {
        Map<String, Object> prop = new LinkedHashMap<>();
        prop.put("type", type);
        prop.put("description", description);
        if (required) {
            prop.put("required", true);
        }
        return prop;
    }

    // --- Argument extraction helpers ---

    private String requireString(JsonNode args, String field) {
        if (!args.has(field) || args.get(field).isNull()) {
            throw new IllegalArgumentException("Missing required argument: " + field);
        }
        return args.get(field).asText();
    }

    private String optString(JsonNode args, String field, String defaultValue) {
        if (args.has(field) && !args.get(field).isNull()) {
            return args.get(field).asText();
        }
        return defaultValue;
    }

    private int optInt(JsonNode args, String field, int defaultValue) {
        if (args.has(field) && !args.get(field).isNull()) {
            return args.get(field).asInt();
        }
        return defaultValue;
    }
}
