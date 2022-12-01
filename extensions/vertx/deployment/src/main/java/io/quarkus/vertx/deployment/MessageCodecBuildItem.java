package io.quarkus.vertx.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class MessageCodecBuildItem extends MultiBuildItem {

    private final String type;
    private final String codec;

    public MessageCodecBuildItem(String type, String codec) {
        this.type = type;
        this.codec = codec;
    }

    public String getType() {
        return type;
    }

    public String getCodec() {
        return codec;
    }
}
