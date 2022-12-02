package org.jboss.resteasy.reactive.client.api;

import java.util.Locale;

public enum LoggingScope {
    NONE,
    REQUEST_RESPONSE,
    ALL;

    public static LoggingScope forName(String name) {
        return LoggingScope.valueOf(name.replace('-', '_').toUpperCase(Locale.ROOT));
    }
}
