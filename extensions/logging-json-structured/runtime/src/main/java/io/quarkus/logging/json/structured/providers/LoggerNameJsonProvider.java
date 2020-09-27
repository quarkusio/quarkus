package io.quarkus.logging.json.structured.providers;

import java.io.IOException;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.structured.JsonGenerator;
import io.quarkus.logging.json.structured.JsonProvider;
import io.quarkus.logging.json.structured.JsonWritingUtils;

public class LoggerNameJsonProvider implements JsonProvider {

    public static final String FIELD_LOGGER_NAME = "loggerName";

    @Override
    public void writeTo(JsonGenerator generator, ExtLogRecord event) throws IOException {
        JsonWritingUtils.writeStringField(generator, FIELD_LOGGER_NAME, event.getLoggerName());
    }
}
