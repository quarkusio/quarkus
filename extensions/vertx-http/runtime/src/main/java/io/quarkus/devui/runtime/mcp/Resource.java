package io.quarkus.devui.runtime.mcp;

import java.net.URI;
import java.util.Optional;

/**
 * Defines a MCP Resource
 *
 * @see https://modelcontextprotocol.io/docs/concepts/resources
 */
public class Resource {
    public URI uri;
    public String name;
    public Optional<String> description;
    public Optional<String> mimeType;
}