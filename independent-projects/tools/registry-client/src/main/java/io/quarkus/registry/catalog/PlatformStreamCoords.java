package io.quarkus.registry.catalog;

public class PlatformStreamCoords {
    final String platformKey;
    final String streamId;

    public static PlatformStreamCoords fromString(String stream) {
        final int colon = stream.indexOf(':');
        String platformKey = colon <= 0 ? null : stream.substring(0, colon);
        String streamId = colon < 0 ? stream : stream.substring(colon + 1);
        return new PlatformStreamCoords(platformKey, streamId);
    }

    public PlatformStreamCoords(String platformKey, String streamId) {
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
