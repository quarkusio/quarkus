package io.quarkus.it.logging.minlevel.set.filter;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.LoggingFilter;

@LoggingFilter(name = "my-filter")
public class TestFilter implements Filter {

    private final String part;

    public TestFilter(@ConfigProperty(name = "my-filter.part") String part) {
        this.part = part;
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return !record.getMessage().contains(part);
    }
}
