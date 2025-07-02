package io.quarkus.devui.runtime.mcp;

/**
 * A resource embedded into a prompt or tool call result.
 *
 * @param resource (must not be {@code null})
 */
record EmbeddedResource(ResourceContents resource) implements Content {

    public EmbeddedResource {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
    }

    @Override
    public Type type() {
        return Type.RESOURCE;
    }

    @Override
    public EmbeddedResource asResource() {
        return this;
    }
}
