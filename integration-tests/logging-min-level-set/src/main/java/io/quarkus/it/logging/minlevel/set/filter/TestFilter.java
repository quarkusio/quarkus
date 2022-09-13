package io.quarkus.it.logging.minlevel.set.filter;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import io.quarkus.logging.LoggingFilter;

@LoggingFilter(name = "my-filter")
public final class TestFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record) {
        return !record.getMessage().contains("TEST");
    }
}
