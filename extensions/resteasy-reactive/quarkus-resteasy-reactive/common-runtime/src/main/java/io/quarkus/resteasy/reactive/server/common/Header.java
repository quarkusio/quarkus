package io.quarkus.resteasy.reactive.server.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
public @interface Header {
    public String name();

    public String value();
}
