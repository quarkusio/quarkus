package io.quarkus.logging.json.structured;

import java.io.IOException;

public interface JsonFactory {

    JsonGenerator createGenerator(StringBuilderWriter writer) throws IOException;
}
