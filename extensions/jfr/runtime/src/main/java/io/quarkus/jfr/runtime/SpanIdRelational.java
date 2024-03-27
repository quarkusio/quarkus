package io.quarkus.jfr.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jdk.jfr.*;

@Relational
@MetadataDefinition
@Name("io.quarkus.SpanId")
@Label("Span ID")
@Description("Link events that have the common Span ID")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SpanIdRelational {
}
