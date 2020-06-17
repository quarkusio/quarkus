package io.quarkus.rest.data.panache.deployment;

import javax.ws.rs.core.UriInfo;

public final class PrivateFields {

    public static final Field URI_INFO = new Field("uriInfo", UriInfo.class);

    public static final class Field {

        private final String name;

        private final Class<?> type;

        public Field(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Class<?> getType() {
            return type;
        }
    }
}
