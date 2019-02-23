package io.quarkus.deployment.builditem;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.builder.item.MultiBuildItem;

/**
 * A configuration converter to register.
 */
public final class ConfigurationCustomConverterBuildItem extends MultiBuildItem {
    private final int priority;
    private final Class<?> type;
    private final Class<? extends Converter<?>> converter;

    public <T> ConfigurationCustomConverterBuildItem(int priority, Class<T> type, Class<? extends Converter<T>> converter) {
        this.priority = priority;
        this.type = type;
        this.converter = converter;
    }

    public int getPriority() {
        return priority;
    }

    public Class<?> getType() {
        return type;
    }

    public Class<? extends Converter<?>> getConverter() {
        return converter;
    }
}
