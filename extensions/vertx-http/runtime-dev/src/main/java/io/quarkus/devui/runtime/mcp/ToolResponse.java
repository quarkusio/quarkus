package io.quarkus.devui.runtime.mcp;

import java.util.Arrays;
import java.util.List;

/**
 * Response to a {@code tools/call} request from the client.
 *
 * @param isError {@code true} if the tool call ended in an error
 * @param content the list of content items (must not be {@code null})
 */
record ToolResponse(boolean isError, List<? extends Content> content) {

    /**
     *
     * @param <C>
     * @param content
     * @return a successful response with the specified content items
     */
    @SafeVarargs
    static <C extends Content> ToolResponse success(C... content) {
        return new ToolResponse(false, Arrays.asList(content));
    }

    /**
     *
     * @param <C>
     * @param content
     * @return a successful response with the specified content items
     */
    static <C extends Content> ToolResponse success(List<C> content) {
        return new ToolResponse(false, content);
    }

    /**
     *
     * @param message
     * @return an unsuccessful response with single text content item
     */
    static ToolResponse error(String message) {
        return new ToolResponse(true, List.of(new TextContent(message)));
    }

    /**
     *
     * @param message
     * @return a successful response with single text content item
     */
    static ToolResponse success(String message) {
        return new ToolResponse(false, List.of(new TextContent(message)));
    }

    ToolResponse {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
    }

}
