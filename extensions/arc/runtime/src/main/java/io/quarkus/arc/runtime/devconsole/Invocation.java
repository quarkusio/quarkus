package io.quarkus.arc.runtime.devconsole;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.quarkus.arc.InjectableBean;

/**
 * Business method invocation.
 */
public class Invocation {

    private final InjectableBean<?> interceptedBean;
    /**
     * Start time in ms
     */
    private final long start;
    /**
     * Duration in ns
     */
    private final long duration;
    private final Method method;
    private final Kind kind;
    private final String message;
    private final List<Invocation> children;

    Invocation(InjectableBean<?> interceptedBean, long start, long duration,
            Method method, Kind kind, String message, List<Invocation> children) {
        this.interceptedBean = interceptedBean;
        this.start = start;
        this.duration = duration;
        this.method = method;
        this.children = children;
        this.kind = kind;
        this.message = message;
    }

    public InjectableBean<?> getInterceptedBean() {
        return interceptedBean;
    }

    public long getStart() {
        return start;
    }

    public String getStartFormatted() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault()).toString();
    }

    public long getDuration() {
        return duration;
    }

    public long getDurationMillis() {
        return TimeUnit.NANOSECONDS.toMillis(duration);
    }

    public Method getMethod() {
        return method;
    }

    public String getDeclaringClassName() {
        return method.getDeclaringClass().getName();
    }

    public List<Invocation> getChildren() {
        return children;
    }

    public Kind getKind() {
        return kind;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return kind + " invocation of " + method;
    }

    public String getPackageName(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            return name.substring(0, lastDot);
        }
        return "";
    }

    public enum Kind {

        BUSINESS,
        PRODUCER,
        DISPOSER,
        OBSERVER

    }

    static class Builder {

        private InjectableBean<?> interceptedBean;
        private long start;
        private long duration;
        private Method method;
        // If async processing and the request context is not propagated a new child can be added when/after the builder is built
        private final List<Builder> children = new CopyOnWriteArrayList<>();
        private Builder parent;
        private Kind kind;
        private String message;

        Builder newChild() {
            Invocation.Builder child = new Builder();
            addChild(child);
            return child;
        }

        Builder setInterceptedBean(InjectableBean<?> bean) {
            this.interceptedBean = bean;
            return this;
        }

        Builder setStart(long start) {
            this.start = start;
            return this;
        }

        Builder setDuration(long duration) {
            this.duration = duration;
            return this;
        }

        Builder setMethod(Method method) {
            this.method = method;
            return this;
        }

        Builder setKind(Kind kind) {
            this.kind = kind;
            return this;
        }

        Builder getParent() {
            return parent;
        }

        Builder setParent(Builder parent) {
            this.parent = parent;
            return this;
        }

        Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        boolean addChild(Builder child) {
            child.setParent(this);
            return children.add(child);
        }

        Invocation build() {
            List<Invocation> invocations = null;
            if (children != null) {
                invocations = new ArrayList<>();
                for (Builder builder : children) {
                    invocations.add(builder.build());
                }
            }
            return new Invocation(interceptedBean, start, duration, method, kind, message, invocations);
        }

    }

}
