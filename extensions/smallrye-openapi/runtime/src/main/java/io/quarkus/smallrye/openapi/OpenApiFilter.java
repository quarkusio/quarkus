package io.quarkus.smallrye.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This extends the MP way to define an `org.eclipse.microprofile.openapi.OASFilter`.
 * Currently in MP, this needs to be added to a config `mp.openapi.filter` and only allows one filter (class) per application.
 * <p>
 * This quarkus specific annotation allows users to annotate a class extending
 * {@link org.eclipse.microprofile.openapi.OASFilter} as an OpenApiFilter. Such annotated classes will automatically be picked
 * up and run during the appropriate {@link RunStage}.
 * <p>
 * The configured RunStages in {@link #stages()} take precedence, if explicitly set. Otherwise, the RunStage from
 * {@link #value()} is used, if explicitly set. Otherwise, the default
 * value of {@link #stages()} is used.
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
     * Intentionally defaults to the deprecated {@link RunStage#RUN} as to not break backwards compatibility.
     */
    RunStage[] stages() default { RunStage.RUN };

    /**
     * When this filter should run, default Runtime.
     *
     * @deprecated Use {@link #stages()} instead.
     */
    @Deprecated(since = "3.32", forRemoval = true)
    RunStage value() default RunStage.RUN;

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
         * @deprecated Consider replacing with either {@link RunStage#RUNTIME_STARTUP} or {@link RunStage#RUNTIME_PER_REQUEST},
         *             depending on the value of the now also deprecated quarkus.smallrye-openapi.always-run-filter.
         */
        @Deprecated(since = "3.32", forRemoval = true)
        RUN,

        /**
         * @deprecated Consider replacing with {@code stages = {OpenApiFilter.RunStage.BUILD,
         *             OpenApiFilter.RunStage.RUNTIME_STARTUP}} or {@code stages = {OpenApiFilter.RunStage.BUILD,
         *             OpenApiFilter.RunStage.RUNTIME_PER_REQUEST}}, depending on the value of the now also deprecated
         *             quarkus.smallrye-openapi.always-run-filter.
         */
        @Deprecated(since = "3.32", forRemoval = true)
        BOTH,

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
