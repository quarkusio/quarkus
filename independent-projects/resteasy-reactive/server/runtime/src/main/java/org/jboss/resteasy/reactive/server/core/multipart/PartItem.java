package org.jboss.resteasy.reactive.server.core.multipart;

import jakarta.ws.rs.core.MediaType;

public final class PartItem {
    private final String name;
    private final MediaType type;
    private final Object value;
    private final String firstParamType;

    public PartItem(String name, MediaType type, Object value, String firstParamType) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.firstParamType = firstParamType;
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

    /**
     * If the value is a parameterized class like a List, it will return the raw representation of the first parameter type.
     * For example, if it's a List<String>, it will return "java.lang.String".
     * If the value is not a parameterized class, it will return an empty string.
     *
     * @return the raw representation of the first parameter type in parameterized classes. Otherwise, empty string.
     */
    public String getFirstParamType() {
        return firstParamType;
    }
}
