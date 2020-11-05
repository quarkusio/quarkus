package io.quarkus.resteasy.reactive.spi;

import java.util.List;

import javax.ws.rs.RuntimeType;

import io.quarkus.builder.item.MultiBuildItem;

public final class MessageBodyReaderBuildItem extends MultiBuildItem implements RuntimeTypeItem {

    private final String className;
    private final String handledClassName;
    private final List<String> mediaTypeStrings;
    private final RuntimeType runtimeType;
    private final boolean builtin;

    public MessageBodyReaderBuildItem(String className, String handledClassName, List<String> mediaTypeStrings) {
        this(className, handledClassName, mediaTypeStrings, null, false);
    }

    public MessageBodyReaderBuildItem(String className, String handledClassName, List<String> mediaTypeStrings,
            RuntimeType runtimeType, boolean builtin) {
        this.className = className;
        this.handledClassName = handledClassName;
        this.mediaTypeStrings = mediaTypeStrings;
        this.runtimeType = runtimeType;
        this.builtin = builtin;
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

    @Override
    public RuntimeType getRuntimeType() {
        return runtimeType;
    }
}
