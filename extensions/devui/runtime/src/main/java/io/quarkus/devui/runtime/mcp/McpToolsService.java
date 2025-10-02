package io.quarkus.devui.runtime.mcp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.mcp.model.tool.Tool;
import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.Usage;

/**
 * This exposes all Dev UI Runtime and Deployment JsonRPC Methods as MCP Tools
 *
 * @see https://modelcontextprotocol.io/specification/2024-11-05/server/tools
 */
@ApplicationScoped
public class McpToolsService {

    @Inject
    JsonRpcRouter jsonRpcRouter;

    @Inject
    McpDevUIJsonRpcService mcpDevUIJsonRpcService;

    @DevMCPEnableByDefault
    @JsonRpcDescription(value = "This list all tools available for MCP")
    public Map<String, List<Tool>> list() {
        if (mcpDevUIJsonRpcService.getMcpServerConfiguration().isEnabled()) {
            List<Tool> tools = toToolList(jsonRpcRouter.getRuntimeMethodsMap().values(),
                    jsonRpcRouter.getDeploymentMethodsMap().values(), Filter.enabled);
            // TODO: Add support for subscriptions
            return Map.of("tools", tools);
        }
        return null;
    }

    public Map<String, List<Tool>> listDisabled() {
        if (mcpDevUIJsonRpcService.getMcpServerConfiguration().isEnabled()) {
            List<Tool> tools = toToolList(jsonRpcRouter.getRuntimeMethodsMap().values(),
                    jsonRpcRouter.getDeploymentMethodsMap().values(), Filter.disabled);

            return Map.of("tools", tools);
        }
        return null;
    }

    public boolean disableTool(String name) {
        mcpDevUIJsonRpcService.disableMethod(name);
        return true;
    }

    public boolean enableTool(String name) {
        mcpDevUIJsonRpcService.enableMethod(name);
        return true;
    }

    private List<Tool> toToolList(Collection<JsonRpcMethod> runtimeMethods,
            Collection<JsonRpcMethod> deploymentMethods, Filter filter) {
        List<Tool> tools = new ArrayList<>();
        for (JsonRpcMethod runtimeJsonRpcMethod : runtimeMethods) {
            addTool(tools, runtimeJsonRpcMethod, filter);
        }
        for (JsonRpcMethod deploymentJsonRpcMethod : deploymentMethods) {
            addTool(tools, deploymentJsonRpcMethod, filter);
        }
        return tools;
    }

    private void addTool(List<Tool> tools, JsonRpcMethod method, Filter filter) {
        if (isEnabled(method, filter)) {
            tools.add(toTool(method));
        }
    }

    private boolean isEnabled(JsonRpcMethod method, Filter filter) {

        if (method.getUsage().contains(Usage.DEV_MCP)) {
            if (mcpDevUIJsonRpcService.isExplicitlyEnabled(method.getMethodName())) {
                return filter.equals(Filter.enabled);
            } else if (mcpDevUIJsonRpcService.isExplicitlyDisabled(method.getMethodName())) {
                return filter.equals(Filter.disabled);
            } else if (filter.equals(Filter.enabled)) {
                return method.isMcpEnabledByDefault();
            } else if (filter.equals(Filter.disabled)) {
                return !method.isMcpEnabledByDefault();
            }
        }
        return false;
    }

    private Tool toTool(JsonRpcMethod jsonRpcMethod) {
        Tool tool = new Tool();
        tool.name = jsonRpcMethod.getMethodName();
        if (jsonRpcMethod.getDescription() != null && !jsonRpcMethod.getDescription().isBlank()) {
            tool.description = jsonRpcMethod.getDescription();
        }

        Map<String, Object> props = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        if (jsonRpcMethod.hasParameters()) {
            for (Map.Entry<String, JsonRpcMethod.Parameter> parameter : jsonRpcMethod.getParameters().entrySet()) {
                Map<String, Object> prop = new HashMap<>();
                JsonRpcMethod.Parameter p = parameter.getValue();
                prop.put("type", mapJavaTypeToJsonType(p.getType()));
                if (p.getDescription() != null && !p.getDescription().isBlank()) {
                    prop.put("description", p.getDescription());
                }
                if (p.isRequired()) {
                    required.add(parameter.getKey());
                }
                props.put(parameter.getKey(), prop);

            }
        }

        tool.inputSchema = Map.of(
                "type", "object",
                "properties", props,
                "required", required);

        return tool;
    }

    private String mapJavaTypeToJsonType(Class<?> clazz) {
        if (clazz == null)
            return "string"; // fallback

        if (clazz == String.class)
            return "string";
        if (clazz == Boolean.class || clazz == boolean.class)
            return "boolean";
        if (clazz == Integer.class || clazz == int.class
                || clazz == Long.class || clazz == long.class
                || clazz == Short.class || clazz == short.class
                || clazz == Byte.class || clazz == byte.class)
            return "integer";
        if (clazz == Double.class || clazz == double.class
                || clazz == Float.class || clazz == float.class)
            return "number";
        if (clazz.isArray() || List.class.isAssignableFrom(clazz))
            return "array";
        if (Map.class.isAssignableFrom(clazz))
            return "object";

        return "string"; // fallback for enums, complex types, etc.
    }
}
