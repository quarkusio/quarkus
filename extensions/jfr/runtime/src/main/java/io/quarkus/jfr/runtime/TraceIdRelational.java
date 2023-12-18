package io.quarkus.jfr.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jdk.jfr.Label;
import jdk.jfr.MetadataDefinition;
import jdk.jfr.Name;
import jdk.jfr.Relational;

@Relational
@MetadataDefinition
@Name("io.quarkus.TraceId")
@Label("Trace ID")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TraceIdRelational {
}
