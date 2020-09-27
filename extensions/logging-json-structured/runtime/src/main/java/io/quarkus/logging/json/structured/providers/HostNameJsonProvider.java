package io.quarkus.logging.json.structured.providers;

import java.io.IOException;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.structured.JsonGenerator;
import io.quarkus.logging.json.structured.JsonProvider;
import io.quarkus.logging.json.structured.JsonWritingUtils;

public class HostNameJsonProvider implements JsonProvider {

    public static final String FIELD_HOST_NAME = "hostName";

    @Override
    public void writeTo(JsonGenerator generator, ExtLogRecord event) throws IOException {
        if (JsonWritingUtils.isNotNullOrEmpty(event.getHostName())) {
            JsonWritingUtils.writeStringField(generator, FIELD_HOST_NAME, event.getHostName());
        }
    }
}
