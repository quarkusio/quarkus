package io.quarkus.logging.json.structured.providers;

import java.io.IOException;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.structured.JsonGenerator;
import io.quarkus.logging.json.structured.JsonProvider;
import io.quarkus.logging.json.structured.JsonWritingUtils;

public class NDCJsonProvider implements JsonProvider {

    public static final String FIELD_NDC = "ndc";

    @Override
    public void writeTo(JsonGenerator generator, ExtLogRecord event) throws IOException {
        if (event.getNdc() != null && !"".equals(event.getNdc())) {
            JsonWritingUtils.writeStringField(generator, FIELD_NDC, event.getNdc());
        }
    }
}
