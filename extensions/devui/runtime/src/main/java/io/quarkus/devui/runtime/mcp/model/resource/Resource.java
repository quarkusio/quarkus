package io.quarkus.devui.runtime.mcp.model.resource;

/**
 * Defines a MCP Resource
 *
 * @see https://modelcontextprotocol.io/docs/concepts/resources
 */
public class Resource {
    private static final String MIME_TYPE_JSON = "application/json";

    public String uri;
    public String name;
    public String description;
    public String mimeType = MIME_TYPE_JSON; // default
}
