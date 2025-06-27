package io.quarkus.devui.runtime.mcp;

/**
 * Defines a MCP Content
 *
 * @see https://modelcontextprotocol.io/docs/concepts/resources
 */
public class Content {
    private static final String MIME_TYPE_JSON = "application/json";

    public String uri;
    public Object text;
    public String mimeType = MIME_TYPE_JSON; // default
}