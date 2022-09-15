package org.jboss.resteasy.reactive.common.processor.scanning;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import java.util.List;

public class ScannedSerializer {

    private final String className;
    private final String handledClassName;
    private final List<String> mediaTypeStrings;
    private final RuntimeType runtimeType;
    private final boolean builtin;
    private final Integer priority;

    public ScannedSerializer(String className, String handledClassName, List<String> mediaTypeStrings) {
        this(className, handledClassName, mediaTypeStrings, null, true, Priorities.USER);
    }

    public ScannedSerializer(String className, String handledClassName, List<String> mediaTypeStrings,
            RuntimeType runtimeType, boolean builtin, Integer priority) {
        this.className = className;
        this.handledClassName = handledClassName;
        this.mediaTypeStrings = mediaTypeStrings;
        this.runtimeType = runtimeType;
        this.builtin = builtin;
        this.priority = priority;
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

    public RuntimeType getRuntimeType() {
        return runtimeType;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public Integer getPriority() {
        return priority;
    }
}
