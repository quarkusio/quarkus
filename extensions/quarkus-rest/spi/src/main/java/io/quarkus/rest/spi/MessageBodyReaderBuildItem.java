package io.quarkus.rest.spi;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

public final class MessageBodyReaderBuildItem extends MultiBuildItem {

    private final String className;
    private final String handledClassName;
    private final List<String> mediaTypeStrings;

    public MessageBodyReaderBuildItem(String className, String handledClassName, List<String> mediaTypeStrings) {
        this.className = className;
        this.handledClassName = handledClassName;
        this.mediaTypeStrings = mediaTypeStrings;
    }

    public String getClassName() {
        return className;
    }

    public String getHandledClassName() {
        return handledClassName;
    }

    public List<String> getMediaTypeStrings() {
        return mediaTypeStrings;
    }
}
