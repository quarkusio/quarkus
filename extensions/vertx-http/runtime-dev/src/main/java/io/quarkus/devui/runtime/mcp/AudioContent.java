package io.quarkus.devui.runtime.mcp;

/**
 * An audio content provided to or from an LLM.
 *
 * @param data a base64-encoded string representing the audio data (must not be {@code null})
 * @param mimeType the mime type of the audio (must not be {@code null})
 */
record AudioContent(String data, String mimeType) implements Content {

    public AudioContent {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (mimeType == null) {
            throw new IllegalArgumentException("mimeType must not be null");
        }
    }

    @Override
    public Type type() {
        return Type.AUDIO;
    }

    @Override
    public AudioContent asAudio() {
        return this;
    }

}
