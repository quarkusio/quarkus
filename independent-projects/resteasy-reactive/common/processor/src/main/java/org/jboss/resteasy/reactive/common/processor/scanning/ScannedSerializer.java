package org.jboss.resteasy.reactive.common.processor.scanning;

import java.util.List;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;

import org.jboss.jandex.ClassInfo;

public class ScannedSerializer {

    private final ClassInfo classInfo;
    private final String className;
    private final String handledClassName;
    private final List<String> mediaTypeStrings;
    private final RuntimeType runtimeType;
    private final boolean builtin;
    private final Integer priority;

    public ScannedSerializer(ClassInfo classInfo, String handledClassName, List<String> mediaTypeStrings) {
        this(classInfo, handledClassName, mediaTypeStrings, null, true, Priorities.USER);
    }

    // used only for testing
    public ScannedSerializer(String className, String handledClassName, List<String> mediaTypeStrings) {
        this(null, className, handledClassName, mediaTypeStrings, null, true, Priorities.USER);
    }

    public ScannedSerializer(ClassInfo classInfo, String handledClassName, List<String> mediaTypeStrings,
            RuntimeType runtimeType, boolean builtin, Integer priority) {
        this(classInfo, classInfo.name().toString(), handledClassName, mediaTypeStrings, runtimeType, builtin,
                priority);
    }

    private ScannedSerializer(ClassInfo classInfo, String className, String handledClassName,
            List<String> mediaTypeStrings, RuntimeType runtimeType, boolean builtin, Integer priority) {
        this.classInfo = classInfo;
        this.className = className;
        this.handledClassName = handledClassName;
        this.mediaTypeStrings = mediaTypeStrings;
        this.runtimeType = runtimeType;
        this.builtin = builtin;
        this.priority = priority;
    }

    // used only for tests

    public ClassInfo getClassInfo() {
        return classInfo;
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
