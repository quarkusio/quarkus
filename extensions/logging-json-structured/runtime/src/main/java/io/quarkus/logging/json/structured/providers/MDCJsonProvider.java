package io.quarkus.logging.json.structured.providers;

import java.io.IOException;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.structured.JsonGenerator;
import io.quarkus.logging.json.structured.JsonProvider;
import io.quarkus.logging.json.structured.JsonWritingUtils;

public class MDCJsonProvider implements JsonProvider {

    public static final String FIELD_MDC = "mdc";

    @Override
    public void writeTo(JsonGenerator generator, ExtLogRecord event) throws IOException {
        JsonWritingUtils.writeMapStringFields(generator, FIELD_MDC, event.getMdcCopy());
    }
}
