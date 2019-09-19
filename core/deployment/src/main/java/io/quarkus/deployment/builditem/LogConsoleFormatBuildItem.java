package io.quarkus.deployment.builditem;

import java.util.Optional;
import java.util.logging.Formatter;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * The log console format build item. Producing this item will cause the logging subsystem to disregard its
 * console logging formatting configuration and use the formatter provided instead. If multiple formatters
 * are enabled at run time, a warning message is printed and only one is used.
 */
public final class LogConsoleFormatBuildItem extends MultiBuildItem {
    private final RuntimeValue<Optional<Formatter>> formatterValue;

    /**
     * Construct a new instance.
     *
     * @param formatterValue the optional formatter run time value to use (must not be {@code null})
     */
    public LogConsoleFormatBuildItem(final RuntimeValue<Optional<Formatter>> formatterValue) {
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
