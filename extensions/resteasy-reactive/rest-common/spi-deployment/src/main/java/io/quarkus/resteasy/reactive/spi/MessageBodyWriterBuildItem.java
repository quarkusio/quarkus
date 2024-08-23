package io.quarkus.resteasy.reactive.spi;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;

import io.quarkus.builder.item.MultiBuildItem;

public final class MessageBodyWriterBuildItem extends MultiBuildItem implements RuntimeTypeItem {

    private final String className;
    private final String handledClassName;
    private final List<String> mediaTypeStrings;
    private final RuntimeType runtimeType;
    private final boolean builtin;
    private final Integer priority;

    public MessageBodyWriterBuildItem(String className, String handledClassName, List<String> mediaTypeStrings) {
        this(className, handledClassName, mediaTypeStrings, null, false, Priorities.USER);
    }

    public MessageBodyWriterBuildItem(String className, String handledClassName, List<String> mediaTypeStrings,
            RuntimeType runtimeType, boolean builtin, Integer priority) {
        this.className = className;
        this.handledClassName = handledClassName;
        this.mediaTypeStrings = mediaTypeStrings;
        this.runtimeType = runtimeType;
        this.builtin = builtin;
        this.priority = priority;
    }

    MessageBodyWriterBuildItem(Builder builder) {
        this.className = builder.className;
        this.handledClassName = builder.handledClassName;
        this.mediaTypeStrings = builder.mediaTypeStrings;
        this.runtimeType = builder.runtimeType;
        this.builtin = builder.builtin;
        this.priority = builder.priority;
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

    public Integer getPriority() {
        return priority;
    }

    public static class Builder {
        private final String className;
        private final String handledClassName;
        private List<String> mediaTypeStrings = Collections.emptyList();
        private RuntimeType runtimeType = null;
        private boolean builtin = false;
        private Integer priority = Priorities.USER;

        public Builder(String className, String handledClassName) {
            this.className = className;
            this.handledClassName = handledClassName;
        }

        public Builder setMediaTypeStrings(List<String> mediaTypeStrings) {
            this.mediaTypeStrings = mediaTypeStrings;
            return this;
        }

        public Builder setRuntimeType(RuntimeType runtimeType) {
            this.runtimeType = runtimeType;
            return this;
        }

        public Builder setBuiltin(boolean builtin) {
            this.builtin = builtin;
            return this;
        }

        public Builder setPriority(Integer priority) {
            this.priority = priority;
            return this;
        }

        public MessageBodyWriterBuildItem build() {
            return new MessageBodyWriterBuildItem(this);
        }
    }
}
