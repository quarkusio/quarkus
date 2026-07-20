package io.quarkus.smallrye.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In MicroProfile OpenAPI, only one implementation of `org.eclipse.microprofile.openapi.OASFilter` can be selected using the
 * `mp.openapi.filter` config property.
 * <br>
 * This quarkus specific annotation allows users to annotate a class extending
 * {@link org.eclipse.microprofile.openapi.OASFilter} as an OpenApiFilter. Such annotated classes will automatically be picked
 * up and run during the appropriate {@link RunStage}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OpenApiFilter {

    /**
     * Default document name which is used for the unnamed default document configuration.
     */
    String DEFAULT_DOCUMENT_NAME = "<default>";

    /**
     * Marker to indicate that a filter should run for any document name.
     */
    String FILTER_RUN_FOR_ANY_DOCUMENT = "<ALL>";

    /**
     * The stages at which this filter should run.
     * <p>
     * Example: {@code @OpenApiFilter(stages = {OpenApiFilter.RunStage.BUILD, OpenApiFilter.RunStage.RUNTIME_STARTUP})}
     * <p>
     */
    RunStage[] stages() default { RunStage.RUNTIME_STARTUP };

    /**
     * Filter with a higher priority will be applied first
     *
     * @return
     */
    int priority() default 1;

    /**
     * Names of the OpenAPI document configurations this filter should be applicable for
     *
     * @return
     */
    String[] documentNames() default { FILTER_RUN_FOR_ANY_DOCUMENT };

    enum RunStage {
        /**
         * Run the filter once at build time.
         */
        BUILD,

        /**
         * Run the filter once at runtime startup.
         */
        RUNTIME_STARTUP,

        /**
         * Run the filter on every request. This makes the OpenAPI document dynamic.
         */
        RUNTIME_PER_REQUEST
    }
}
