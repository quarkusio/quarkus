package org.jboss.shamrock.runtime.configuration;

import java.io.File;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A configuration converter for {@link File}.
 */
public final class FileConverter implements Converter<File> {
    public File convert(final String value) {
        return value == null || value.isEmpty() ? null : new File(value);
    }
}
