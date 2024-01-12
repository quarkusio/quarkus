package io.quarkus.smallrye.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This extends the MP way to define an `org.eclipse.microprofile.openapi.OASFilter`.
 * Currently in MP, this needs to be added to a config `mp.openapi.filter` and only allows one filter (class) per application.
 *
 * This Annotation, that is Quarkus specific, will allow users to annotate one or more classes and that will be
 * all that is needed to include the filter. (No config needed). Filters still need to extend.
 *
 * @see https://download.eclipse.org/microprofile/microprofile-open-api-3.1.1/microprofile-openapi-spec-3.1.1.html#_oasfilter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OpenApiFilter {
    RunStage value() default RunStage.RUN; // When this filter should run, default Runtime

    /**
     * Filter with a higher priority will applied first
     *
     * @return
     */
    int priority() default 1;

    static enum RunStage {
        BUILD,
        RUN,
        BOTH
    }
}