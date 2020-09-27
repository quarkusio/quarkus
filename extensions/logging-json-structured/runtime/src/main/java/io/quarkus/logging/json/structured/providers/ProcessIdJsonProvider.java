package io.quarkus.logging.json.structured.providers;

import java.io.IOException;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.structured.JsonGenerator;
import io.quarkus.logging.json.structured.JsonProvider;
import io.quarkus.logging.json.structured.JsonWritingUtils;

public class ProcessIdJsonProvider implements JsonProvider {

    public static final String FIELD_PROCESS_ID = "processId";

    @Override
    public void writeTo(JsonGenerator generator, ExtLogRecord event) throws IOException {
        if (event.getProcessId() >= 0) {
            JsonWritingUtils.writeNumberField(generator, FIELD_PROCESS_ID, event.getProcessId());
        }
    }
}
