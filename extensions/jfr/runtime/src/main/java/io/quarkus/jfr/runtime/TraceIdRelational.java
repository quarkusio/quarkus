package io.quarkus.jfr.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.MetadataDefinition;
import jdk.jfr.Name;
import jdk.jfr.Relational;

/**
 * This is an annotation that associates multiple events in JFR by TraceId.
 * Fields that can be annotated with this annotation must be in the String class.
 */
@Relational
@MetadataDefinition
@Name("io.quarkus.TraceId")
@Label("Trace ID")
@Description("Links traces with the same ID together")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TraceIdRelational {
}
