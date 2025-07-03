package io.quarkus.devui.runtime.mcp;

/**
 * Represents text/binary data of a resource.
 */
public sealed interface ResourceContents permits TextResourceContents, BlobResourceContents {

    /**
     *
     * @return the type of the resource
     */
    Type type();

    /**
     * Casts and returns this object as text resource contents, or throws an {@link IllegalArgumentException} if the content
     * object does not represent a {@link TextResourceContents}.
     *
     * @return the text content
     */
    TextResourceContents asText();

    /**
     * Casts and returns this object as binary resource contents, or throws an {@link IllegalArgumentException} if the content
     * object does not represent a {@link BlobResourceContents}.
     *
     * @return the binary content
     */
    BlobResourceContents asBlob();

    enum Type {
        TEXT,
        BLOB
    }

}
