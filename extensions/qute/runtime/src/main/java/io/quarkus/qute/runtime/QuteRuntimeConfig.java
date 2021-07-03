package io.quarkus.qute.runtime;

import java.util.Optional;

import io.quarkus.qute.Results;
import io.quarkus.qute.TemplateException;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "qute", phase = ConfigPhase.RUN_TIME)
public class QuteRuntimeConfig {

    /**
     * The strategy used when a standalone expression evaluates to a "not found" value at runtime and
     * the {@code io.quarkus.qute.strict-rendering} config property is set to {@code false}
     * <p>
     * This strategy is never used when evaluating section parameters, e.g. <code>{#if foo.name}</code>. In such case, it's the
     * responsibility of the section to handle this situation appropriately.
     * <p>
     * By default, the {@code NOT_FOUND} constant is written to the output. However, in the development mode the
     * {@link PropertyNotFoundStrategy#THROW_EXCEPTION} is used by default, i.e. when the strategy is not specified.
     */
    @ConfigItem
    public Optional<PropertyNotFoundStrategy> propertyNotFoundStrategy;
    /**
     * Specify whether the parser should remove standalone lines from the output. A standalone line is a line that contains at
     * least one section tag, parameter declaration, or comment but no expression and no non-whitespace character.
     */
    @ConfigItem(defaultValue = "true")
    public boolean removeStandaloneLines;

    /**
     * If set to {@code true} then any expression that is evaluated to a {@link Results.NotFound} value will always result in a
     * {@link TemplateException} and the rendering is aborted.
     * <p>
     * Note that the {@code quarkus.qute.property-not-found-strategy} config property is completely ignored if strict rendering
     * is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean strictRendering;

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
