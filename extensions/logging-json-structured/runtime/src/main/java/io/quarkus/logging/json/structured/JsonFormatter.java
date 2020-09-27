package io.quarkus.logging.json.structured;

import java.util.List;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.structured.jackson.JacksonJsonFactory;

public class JsonFormatter extends ExtFormatter {
    private final StringBuilderWriter writer = new StringBuilderWriter();
    private final List<JsonProvider> providers;
    private final JsonFactory jsonFactory;
    private String recordDelimiter;

    public JsonFormatter(List<JsonProvider> providers, JsonStructuredConfig config) {
        this.providers = providers;
        this.jsonFactory = new JacksonJsonFactory(true);
        this.recordDelimiter = config.recordDelimiter;
    }

    @Override
    public String format(ExtLogRecord record) {
        try {
            try (JsonGenerator generator = this.jsonFactory.createGenerator(writer)) {
                generator.writeStartObject();
                for (JsonProvider provider : this.providers) {
                    provider.writeTo(generator, record);
                }
                generator.writeEndObject();
                generator.flush();
                if (recordDelimiter != null) {
                    writer.write(recordDelimiter);
                }
            }
            return writer.toString();
        } catch (Exception e) {
            // Wrap and rethrow
            throw new RuntimeException(e);
        } finally {
            // Clear the writer for the next format
            writer.clear();
        }
    }
}
