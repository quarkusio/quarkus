package io.quarkus.runtime.execution;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.wildfly.common.Assert;

import com.oracle.svm.core.annotate.AlwaysInline;

/**
 * The application execution context, used to pass data from earlier stages to later stages.
 */
public class ExecutionContext {
    private static final String[] NO_STRINGS = new String[0];

    /**
     * The empty execution context.
     */
    public static final ExecutionContext EMPTY = new ExecutionContext();

    private final ExecutionContext parent;

    private ExecutionContext() {
        parent = null;
    }

    /**
     * Construct a new instance.
     *
     * @param parent the parent context (must not be {@code null})
     */
    protected ExecutionContext(final ExecutionContext parent) {
        Assert.checkNotNullParam("parent", parent);
        this.parent = parent;
    }

    /**
     * Get the given view of the execution context, if possible.
     *
     * @param clazz the view type class (must not be {@code null})
     * @param <C> the view type
     * @return the view of the execution context
     * @throws IllegalStateException if the context type is not available
     */
    @AlwaysInline("Trivial startup operations")
    public final <C extends ExecutionContext> C as(Class<C> clazz) throws IllegalStateException {
        if (clazz.isInstance(this)) {
            return clazz.cast(this);
        } else if (parent == null) {
            throw new IllegalStateException("Context " + clazz + " not available");
        } else {
            return parent.as(clazz);
        }
    }

    /**
     * Get the given view of the execution context, if possible.
     *
     * @param clazz the view type class (must not be {@code null})
     * @param <C> the view type
     * @return the view of the execution context
     * @throws IllegalStateException if the context type is not available
     */
    @AlwaysInline("Trivial startup operations")
    public <C extends ExecutionContext> Optional<C> optionallyAs(final Class<C> clazz) {
        if (clazz.isInstance(this)) {
            return Optional.of(clazz.cast(this));
        } else if (parent == null) {
            return Optional.empty();
        } else {
            return parent.optionallyAs(clazz);
        }
    }

    /**
     * Get the parent context.
     *
     * @return the parent context
     */
    @AlwaysInline("Trivial startup operations")
    protected final ExecutionContext getParent() {
        return parent;
    }

    public ExecutionContext withValue(String key, Object val) {
        Assert.checkNotNullParam("key", key);
        Assert.checkNotNullParam("val", val);
        return new MapValuesExecutionContext(this, Collections.singletonMap(key, val));
    }

    public ExecutionContext withValues(String key1, Object val1, String key2, Object val2) {
        Assert.checkNotNullParam("key1", key1);
        Assert.checkNotNullParam("val1", val1);
        Assert.checkNotNullParam("key2", key2);
        Assert.checkNotNullParam("val2", val2);
        // todo: replace with Map.of(...)
        Map<String, Object> map = new HashMap<>();
        map.put(key1, val1);
        map.put(key2, val2);
        return new MapValuesExecutionContext(this, map);
    }

    public ExecutionContext withoutValue(String key) {
        Assert.checkNotNullParam("key", key);
        return new MapValuesExecutionContext(this, Collections.singletonMap(key, null));
    }

    public ExecutionContext withArguments(String[] args) {
        Assert.checkNotNullParam("args", args);
        return new ArgumentsExecutionContext(this, args.clone());
    }

    public ExecutionContext withArguments(Collection<String> args) {
        Assert.checkNotNullParam("args", args);
        return new ArgumentsExecutionContext(this, args.toArray(NO_STRINGS), false);
    }

    public ExecutionContext withValueAndArguments(String key, Object val, String[] args) {
        return withValue(key, val).withArguments(args);
    }

    public ExecutionContext withValues(final Map<String, Object> values) {
        return values.isEmpty() ? this : new MapValuesExecutionContext(this, values);
    }

    @Deprecated
    public ExecutionContext withCloseable(final AutoCloseable closeable) {
        return closeable == null ? this : new CloseableExecutionContext(this, closeable);
    }

    public final boolean equals(final Object obj) {
        return super.equals(obj);
    }

    public final int hashCode() {
        return super.hashCode();
    }

    protected void appendDescription(StringBuilder b) {
        b.append("execution context");
    }

    private StringBuilder toString(StringBuilder b) {
        if (parent == null) {
            b.append("root execution context");
        } else {
            appendDescription(b);
            b.append(" of\n");
            parent.toString(b);
        }
        return b;
    }

    public final String toString() {
        return toString(new StringBuilder()).toString();
    }
}
