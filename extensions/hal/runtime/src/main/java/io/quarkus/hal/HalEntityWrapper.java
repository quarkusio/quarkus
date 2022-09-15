package io.quarkus.hal;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Link;

/**
 * The Hal entity wrapper that includes the entity and the Hal links.
 *
 * This type is serialized into Json using:
 * - the JSON-B serializer: {@link HalEntityWrapperJsonbSerializer}
 * - the Jackson serializer: {@link HalEntityWrapperJacksonSerializer}
 */
public class HalEntityWrapper extends HalWrapper {

    private final Object entity;

    public HalEntityWrapper(Object entity, Link... links) {
        this(entity, new HashMap<>());

        addLinks(links);
    }

    public HalEntityWrapper(Object entity, Map<String, HalLink> links) {
        super(links);

        this.entity = entity;
    }

    public Object getEntity() {
        return entity;
    }
}
