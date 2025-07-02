package io.quarkus.devui.runtime.mcp;

/**
 * A content provided to or from an LLM.
 */
public sealed interface Content permits TextContent, ImageContent, EmbeddedResource, AudioContent {

    /**
     *
     * @return the type of the content
     */
    Type type();

    /**
     * Casts and returns this object as a text content, or throws an {@link IllegalArgumentException} if the content object does
     * not represent a {@link TextContent}.
     *
     * @return the text content
     */
    default TextContent asText() {
        throw new IllegalArgumentException("Not a text");
    }

    /**
     * Casts and returns this object as an image content, or throws an {@link IllegalArgumentException} if the content object
     * does not represent a {@link ImageContent}.
     *
     * @return the image content
     */
    default ImageContent asImage() {
        throw new IllegalArgumentException("Not an image");
    }

    /**
     * Casts and returns this object as an embedded resource content, or throws an {@link IllegalArgumentException} if the
     * content object does not represent a {@link EmbeddedResource}.
     *
     * @return the resource
     */
    default EmbeddedResource asResource() {
        throw new IllegalArgumentException("Not a resource");
    }

    /**
     * Casts and returns this object as an audio content, or throws an {@link IllegalArgumentException} if the content object
     * does not represent a {@link AudioContent}.
     *
     * @return the audio content
     */
    default AudioContent asAudio() {
        throw new IllegalArgumentException("Not an audio");
    }

    enum Type {
        TEXT,
        IMAGE,
        RESOURCE,
        AUDIO
    }

}
