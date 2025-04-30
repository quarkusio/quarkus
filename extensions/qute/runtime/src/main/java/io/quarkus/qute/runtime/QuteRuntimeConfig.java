package io.quarkus.qute.runtime;

import java.util.Optional;

import io.quarkus.qute.Results;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.qute")
public interface QuteRuntimeConfig {

    /**
     * The strategy used when a standalone expression evaluates to a "not found" value at runtime and
     * the {@code quarkus.qute.strict-rendering} config property is set to {@code false}
     * <p>
     * This strategy is never used when evaluating section parameters, e.g. <code>{#if foo.name}</code>. In such case, it's the
     * responsibility of the section to handle this situation appropriately.
     * <p>
     * By default, the {@code NOT_FOUND} constant is written to the output. However, in the development mode the
     * {@link PropertyNotFoundStrategy#THROW_EXCEPTION} is used by default, i.e. when the strategy is not specified.
     */
    Optional<PropertyNotFoundStrategy> propertyNotFoundStrategy();

    /**
     * Specify whether the parser should remove standalone lines from the output. A standalone line is a line that contains at
     * least one section tag, parameter declaration, or comment but no expression and no non-whitespace character.
     */
    @WithDefault("true")
    boolean removeStandaloneLines();

    /**
     * If set to {@code true} then any expression that is evaluated to a {@link Results.NotFound} value will always result in a
     * {@link TemplateException} and the rendering is aborted.
     * <p>
     * Note that the {@code quarkus.qute.property-not-found-strategy} config property is completely ignored if strict rendering
     * is enabled.
     */
    @WithDefault("true")
    boolean strictRendering();

    /**
     * The global rendering timeout in milliseconds. It is used if no {@code timeout} template instance attribute is set.
     */
    @WithDefault("10000")
    long timeout();

    /**
     * If set to {@code true} then the timeout should also be used for asynchronous rendering methods, such as
     * {@link TemplateInstance#createUni()} and {@link TemplateInstance#renderAsync()}.
     */
    @WithDefault("true")
    boolean useAsyncTimeout();

    public enum PropertyNotFoundStrategy {
        /**
         * Output the {@code NOT_FOUND} constant.
         */
        DEFAULT,
        /**
         * No operation - no output.
         */
        NOOP,
        /**
         * Throw a {@link TemplateException}.
         */
        THROW_EXCEPTION,
        /**
         * Output the original expression string, e.g. <code>{foo.name}</code>.
         */
        OUTPUT_ORIGINAL
    }

}
