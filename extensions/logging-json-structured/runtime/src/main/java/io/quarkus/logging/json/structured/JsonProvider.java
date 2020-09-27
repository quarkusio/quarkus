package io.quarkus.logging.json.structured;

import java.io.IOException;

import org.jboss.logmanager.ExtLogRecord;

public interface JsonProvider {

    void writeTo(JsonGenerator generator, ExtLogRecord event) throws IOException;
}
