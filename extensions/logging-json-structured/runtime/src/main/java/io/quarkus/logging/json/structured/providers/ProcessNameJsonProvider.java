package io.quarkus.logging.json.structured.providers;

import java.io.IOException;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.structured.JsonGenerator;
import io.quarkus.logging.json.structured.JsonProvider;
import io.quarkus.logging.json.structured.JsonWritingUtils;

public class ProcessNameJsonProvider implements JsonProvider {

    public static final String FIELD_PROCESS_NAME = "processName";

    @Override
    public void writeTo(JsonGenerator generator, ExtLogRecord event) throws IOException {
        if (JsonWritingUtils.isNotNullOrEmpty(event.getProcessName())) {
            JsonWritingUtils.writeStringField(generator, FIELD_PROCESS_NAME, event.getProcessName());
        }
    }
}
