package org.jboss.resteasy.reactive.common.headers;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.ext.RuntimeDelegate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class EntityTagDelegate implements RuntimeDelegate.HeaderDelegate<EntityTag> {
    public static final EntityTagDelegate INSTANCE = new EntityTagDelegate();

    public EntityTag fromString(String value) throws IllegalArgumentException {
        if (value == null)
            throw new IllegalArgumentException("param was null");
        boolean weakTag = false;
        if (value.startsWith("W/")) {
            weakTag = true;
            value = value.substring(2);
        }
        if (value.startsWith("\"")) {
            value = value.substring(1);
        }
        if (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }
        return new EntityTag(value, weakTag);
    }

    public String toString(EntityTag value) {
        String weak = value.isWeak() ? "W/" : "";
        return weak + '"' + value.getValue() + '"';
    }

}
