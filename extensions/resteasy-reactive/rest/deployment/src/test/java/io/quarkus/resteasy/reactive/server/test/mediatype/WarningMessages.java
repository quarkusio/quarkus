package io.quarkus.resteasy.reactive.server.test.mediatype;

import java.util.logging.LogRecord;

final class WarningMessages {

    private WarningMessages() {
    }

    /**
     * @return the message of a record logged with a printf-style pattern, with its parameters applied
     */
    static String format(LogRecord record) {
        Object[] parameters = record.getParameters();
        if (parameters == null || parameters.length == 0) {
            return record.getMessage();
        }
        return String.format(record.getMessage(), parameters);
    }
}
