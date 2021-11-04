package io.quarkus.resteasy.reactive.server.runtime.multipart;

import javax.ws.rs.core.MediaType;

public final class PartItem {
    private final String name;
    private final MediaType type;
    private final Object value;

    public PartItem(String name, MediaType type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public MediaType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
}
