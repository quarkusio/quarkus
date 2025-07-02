package io.quarkus.devui.runtime.mcp;

/**
 * A text content provided to or from an LLM.
 *
 * @param text (must not be {@code null})
 */
record TextContent(String text) implements Content {

    TextContent {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
    }

    @Override
    public Type type() {
        return Type.TEXT;
    }

    @Override
    public TextContent asText() {
        return this;
    }

}
