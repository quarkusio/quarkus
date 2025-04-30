package io.quarkus.deployment.builditem;

import java.util.Optional;
import java.util.logging.Formatter;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.smallrye.common.constraint.Assert;

/**
 * The log file format build item. Producing this item will cause the logging subsystem to disregard its
 * file logging formatting configuration and use the formatter provided instead. If multiple formatters
 * are enabled at runtime, a warning message is printed and only one is used.
 */
public final class LogFileFormatBuildItem extends MultiBuildItem {
    private final RuntimeValue<Optional<Formatter>> formatterValue;

    /**
     * Construct a new instance.
     *
     * @param formatterValue the optional formatter runtime value to use (must not be {@code null})
     */
    public LogFileFormatBuildItem(final RuntimeValue<Optional<Formatter>> formatterValue) {
        this.formatterValue = Assert.checkNotNullParam("formatterValue", formatterValue);
    }

    /**
     * Get the formatter value.
     *
     * @return the formatter value
     */
    public RuntimeValue<Optional<Formatter>> getFormatterValue() {
        return formatterValue;
    }
}
