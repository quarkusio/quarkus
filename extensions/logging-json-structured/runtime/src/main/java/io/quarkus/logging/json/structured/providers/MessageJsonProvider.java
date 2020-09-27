package io.quarkus.logging.json.structured.providers;

import java.io.IOException;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.structured.JsonGenerator;
import io.quarkus.logging.json.structured.JsonProvider;
import io.quarkus.logging.json.structured.JsonWritingUtils;

public class MessageJsonProvider extends ExtFormatter implements JsonProvider {

    public static final String FIELD_MESSAGE = "message";

    @Override
    public void writeTo(JsonGenerator generator, ExtLogRecord event) throws IOException {
        JsonWritingUtils.writeStringField(generator, FIELD_MESSAGE, formatMessage(event));
    }

    @Override
    public String format(ExtLogRecord record) {
        return null;
    }
}
