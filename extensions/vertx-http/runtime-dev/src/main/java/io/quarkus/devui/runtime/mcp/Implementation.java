package io.quarkus.devui.runtime.mcp;

/**
 * The name and version of an MCP implementation.
 *
 * @param name (must not be {@code null})
 * @param version (must not be {@code null})
 */
record Implementation(String name, String version) {

    public Implementation {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (version == null) {
            throw new IllegalArgumentException("version must not be null");
        }
    }

}
