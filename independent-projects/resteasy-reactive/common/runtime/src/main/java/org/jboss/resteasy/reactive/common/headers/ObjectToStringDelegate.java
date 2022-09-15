package org.jboss.resteasy.reactive.common.headers;

import jakarta.ws.rs.ext.RuntimeDelegate;
import java.util.Objects;

public class ObjectToStringDelegate implements RuntimeDelegate.HeaderDelegate<Object> {
    public static final ObjectToStringDelegate INSTANCE = new ObjectToStringDelegate();

    @Override
    public Object fromString(String value) {
        if (value == null)
            throw new IllegalArgumentException("Param was null");
        throw new IllegalArgumentException("Does not support fromString");
    }

    @Override
    public String toString(Object value) {
        if (value == null)
            throw new IllegalArgumentException("Param was null");
        return Objects.toString(value);
    }
}
