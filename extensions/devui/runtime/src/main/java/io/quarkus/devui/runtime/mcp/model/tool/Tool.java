package io.quarkus.devui.runtime.mcp.model.tool;

import java.util.Map;

/**
 * Defines a MCP Tool
 */
public class Tool {
    public String name;
    public String description;
    public Map<String, Object> inputSchema;
    public Map<String, Object> annotations;

}