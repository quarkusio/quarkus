package io.quarkus.resteasy.reactive.server.common;

public @interface Header {
    public String name();

    public String value();
}
