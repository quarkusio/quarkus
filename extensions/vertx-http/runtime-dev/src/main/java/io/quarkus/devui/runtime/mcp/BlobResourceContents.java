package io.quarkus.devui.runtime.mcp;

import java.util.Base64;

/**
 * Binary data of a resource.
 *
 * @param uri (must not be {@code null})
 * @param blob a base64-encoded string representing the binary data of the item (must not be {@code null})
 * @param mimeType the mime type of this resource
 */
record BlobResourceContents(String uri, String blob, String mimeType) implements ResourceContents {

    /**
     *
     * @param uri
     * @param blob
     * @return a new binary resource contents
     */
    public static BlobResourceContents create(String uri, String blob) {
        return new BlobResourceContents(uri, blob, null);
    }

    /**
     *
     * @param uri
     * @param blob
     * @return a new binary resource contents
     */
    public static BlobResourceContents create(String uri, byte[] blob) {
        return new BlobResourceContents(uri, Base64.getMimeEncoder().encodeToString(blob), null);
    }

    public BlobResourceContents {
        if (uri == null) {
            throw new IllegalArgumentException("uri must not be null");
        }
        if (blob == null) {
            throw new IllegalArgumentException("blob must not be null");
        }
    }

    @Override
    public Type type() {
        return Type.BLOB;
    }

    @Override
    public TextResourceContents asText() {
        throw new IllegalArgumentException("Not a text");
    }

    @Override
    public BlobResourceContents asBlob() {
        return this;
    }

}
