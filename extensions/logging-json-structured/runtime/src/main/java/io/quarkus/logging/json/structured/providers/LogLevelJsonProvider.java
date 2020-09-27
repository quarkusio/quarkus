package io.quarkus.logging.json.structured.providers;

import java.io.IOException;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.structured.JsonGenerator;
import io.quarkus.logging.json.structured.JsonProvider;
import io.quarkus.logging.json.structured.JsonWritingUtils;

public class LogLevelJsonProvider implements JsonProvider {
    public static final String FIELD_LEVEL = "level";

    @Override
    public void writeTo(JsonGenerator generator, ExtLogRecord event) throws IOException {
        JsonWritingUtils.writeStringField(generator, FIELD_LEVEL, event.getLevel().toString());
    }
}
