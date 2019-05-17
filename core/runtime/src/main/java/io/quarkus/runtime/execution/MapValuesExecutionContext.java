package io.quarkus.runtime.execution;

import java.util.Map;

import org.wildfly.common.Assert;

/**
 * A map values execution context, used for classic key-value object propagation.
 */
public final class MapValuesExecutionContext extends ExecutionContext {
    private final Map<String, Object> values;

    MapValuesExecutionContext(final ExecutionContext parent, final Map<String, Object> values) {
        super(parent);
        Assert.checkNotNullParam("values", values);
        Assert.checkNotEmptyParam("values", values);
        this.values = values;
    }

    public Object getValue(String key) {
        if (values.containsKey(key)) {
            return values.get(key);
        } else {
            MapValuesExecutionContext parent = getParent().as(MapValuesExecutionContext.class);
            if (parent == null) {
                return null;
            } else {
                return parent.getValue(key);
            }
        }
    }
}
