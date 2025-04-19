package io.quarkus.devui.runtime.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.devui.runtime.comms.JsonRpcRouter;

/**
 * This exposes all Dev UI JsonRPC Methods as Tools
 */
@ApplicationScoped
public class MCPToolsService {

    @Inject
    JsonRpcRouter jsonRpcRouter;

    public Map<String, List<Tool>> list() {
        List<Tool> tools = new ArrayList<>();
        tools.addAll(toToolList(jsonRpcRouter.getRuntimeMethods()));
        tools.addAll(toToolList(jsonRpcRouter.getDeploymentMethods()));
        tools.addAll(toToolList(jsonRpcRouter.getRecordedMethods()));
        return Map.of("tools", tools);
    }

    private List<Tool> toToolList(Set<String> methods) {
        List<Tool> tools = new ArrayList<>();
        for (String method : methods) {
            Tool tool = new Tool();
            tool.name = method;
            tool.description = autoDescriptionFromMethodName(method);
            tools.add(tool);
        }
        return tools;
    }

    /**
     * Generate a user-friendly description from the method name.
     */
    private String autoDescriptionFromMethodName(String fullMethodName) {
        String[] parts = fullMethodName.split("/");
        if (parts.length != 2)
            return "No description";

        String ns = parts[0];
        String methodName = preprocessKnownAcronyms(parts[1]);

        // Split camelCase and clean up
        String readableMethod = methodName
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();

        readableMethod = capitalizeFirst(readableMethod);

        if (ns.startsWith("devui-")) {
            String feature = ns.substring("devui-".length());
            return readableMethod + " for the " + feature;
        }

        String[] gavParts = ns.split("\\.");
        String artifactId = gavParts[gavParts.length - 1];
        return readableMethod + " from the " + artifactId + " extension";
    }

    private String capitalizeFirst(String input) {
        return input.isEmpty() ? input
                : Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    private String preprocessKnownAcronyms(String input) {
        Map<String, String> acronyms = Map.of(
                "GraphQL", "GraphQL",
                "HTML", "HTML",
                "JSON", "JSON",
                "HTTP", "HTTP",
                "URL", "URL",
                "XML", "XML",
                "CPU", "CPU",
                "ID", "ID");
        for (String acronym : acronyms.keySet()) {
            input = input.replaceAll(acronym, " " + acronym + " ");
        }
        return input;
    }
}
