package io.quarkus.runtime.logging;

import java.util.logging.Level;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.logmanager.LogContext;

/**
 * A simple converter for logging levels.
 */
public final class LevelConverter implements Converter<Level> {
    public LevelConverter() {
    }

    public Level convert(final String value) {
        if (value == null || value.isEmpty())
            return null;
        return LogContext.getLogContext().getLevelForName(value);
    }
}
