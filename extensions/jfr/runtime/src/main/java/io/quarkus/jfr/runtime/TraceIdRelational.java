package io.quarkus.jfr.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jdk.jfr.*;

@Relational
@MetadataDefinition
@Name("io.quarkus.TraceId")
@Label("Trace ID")
@Description("Link events that have the common Trace ID")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TraceIdRelational {
}
