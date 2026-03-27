package io.quarkus.deployment.builditem;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Formatter;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.logging.NamedHandlerType;
import io.smallrye.common.constraint.Assert;

/**
 * Named handler log format build item. Producing this item provides per-named-handler formatters
 * for all named logging handler types (console, file, syslog, socket).
 * <p>
 * The outer map key is the {@link NamedHandlerType} identifying the handler type.
 * The inner map key is the handler name.
 */
public final class LogNamedHandlerFormatBuildItem extends MultiBuildItem {

    private final RuntimeValue<Map<NamedHandlerType, Map<String, Optional<Formatter>>>> namedFormattersValue;

    public LogNamedHandlerFormatBuildItem(
            final RuntimeValue<Map<NamedHandlerType, Map<String, Optional<Formatter>>>> namedFormattersValue) {
        this.namedFormattersValue = Assert.checkNotNullParam("namedFormattersValue", namedFormattersValue);
    }

    public RuntimeValue<Map<NamedHandlerType, Map<String, Optional<Formatter>>>> getNamedFormattersValue() {
        return namedFormattersValue;
    }
}
