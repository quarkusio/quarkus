package io.quarkus.resteasy.reactive.spi;

import java.util.List;

import javax.ws.rs.RuntimeType;

import io.quarkus.builder.item.MultiBuildItem;

public final class MessageBodyWriterBuildItem extends MultiBuildItem implements RuntimeTypeItem {

    private final String className;
    private final String handledClassName;
    private final List<String> mediaTypeStrings;
    private final RuntimeType runtimeType;
    private final boolean builtin;

    public MessageBodyWriterBuildItem(String className, String handledClassName, List<String> mediaTypeStrings) {
        this(className, handledClassName, mediaTypeStrings, null, false);
    }

    public MessageBodyWriterBuildItem(String className, String handledClassName, List<String> mediaTypeStrings,
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

    public boolean isBuiltin() {
        return builtin;
    }
}
