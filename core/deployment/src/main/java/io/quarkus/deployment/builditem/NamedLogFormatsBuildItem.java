package io.quarkus.deployment.builditem;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Formatter;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * The named log formatters build item. Producing this item will cause the logging subsystem to disregard its
 * logging formatting configuration and use the formatter provided instead. If multiple formatters
 * are enabled at run time, a warning message is printed and only one is used. Reserved names are console, file, syslog,
 * socket.
 */
public final class NamedLogFormatsBuildItem extends MultiBuildItem {
    private final RuntimeValue<Optional<Map<String, Formatter>>> namedFormattersValue;

    /**
     * Construct a new instance.
     *
     * @param namedFormattersValue the optional named formatters run time value to use (must not be {@code null})
     */
    public NamedLogFormatsBuildItem(final RuntimeValue<Optional<Map<String, Formatter>>> namedFormattersValue) {
        this.namedFormattersValue = Assert.checkNotNullParam("namedFormattersValue", namedFormattersValue);
    }

    /**
     * Get the named formatters value.
     *
     * @return the named formatters value
     */
    public RuntimeValue<Optional<Map<String, Formatter>>> getNamedFormattersValue() {
        return namedFormattersValue;
    }
}
