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
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.Usage;

/**
 * This exposes all Dev UI Runtime and Deployment JsonRPC Methods as MCP Tools
 *
 * @see https://modelcontextprotocol.io/specification/2024-11-05/server/tools
 */
@ApplicationScoped
public class MCPToolsService {

    @Inject
    JsonRpcRouter jsonRpcRouter;

    @JsonRpcDescription("This list all tools available for MCP")
    public Map<String, List<Tool>> list() {
        List<Tool> tools = toToolList(jsonRpcRouter.getRuntimeMethodsMap().values(),
                jsonRpcRouter.getDeploymentMethodsMap().values());
        // TODO: Add support for subscriptions
        return Map.of("tools", tools);
    }

    private List<Tool> toToolList(Collection<JsonRpcMethod> runtimeMethods,
            Collection<JsonRpcMethod> deploymentMethods) {
        List<Tool> tools = new ArrayList<>();
        for (JsonRpcMethod runtimeJsonRpcMethod : runtimeMethods) {
            addTool(tools, runtimeJsonRpcMethod);
        }
        for (JsonRpcMethod deploymentJsonRpcMethod : deploymentMethods) {
            addTool(tools, deploymentJsonRpcMethod);
        }
        return tools;
    }

    private void addTool(List<Tool> tools, JsonRpcMethod method) {
        Tool tool = toTool(method);
        if (tool != null) {
            tools.add(tool);
        }
    }

    private Tool toTool(JsonRpcMethod jsonRpcMethod) {

        if (!jsonRpcMethod.getUsage().contains(Usage.DEV_MCP)) {
            return null;
        }

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
                required.add(parameter.getKey()); // TODO: Check for optional here
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