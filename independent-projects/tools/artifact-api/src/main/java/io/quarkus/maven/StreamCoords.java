package io.quarkus.maven;

public class StreamCoords {
    final String platformKey;
    final String streamId;

    public static StreamCoords fromString(String stream) {
        final int colon = stream.indexOf(':');
        String platformKey = colon <= 0 ? null : stream.substring(0, colon);
        String streamId = colon < 0 ? stream : stream.substring(colon + 1);
        return new StreamCoords(platformKey, streamId);
    }

    public StreamCoords(String platformKey, String streamId) {
        this.platformKey = platformKey;
        this.streamId = streamId;
    }

    public String getPlatformKey() {
        return platformKey;
    }

    public String getStreamId() {
        return streamId;
    }

    @Override
    public String toString() {
        return "StreamCoords{" +
                "platformKey='" + platformKey + '\'' +
                ", streamId='" + streamId + '\'' +
                '}';
    }
}
