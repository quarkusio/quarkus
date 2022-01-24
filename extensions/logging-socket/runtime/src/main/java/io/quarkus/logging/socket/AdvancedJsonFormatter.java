package io.quarkus.logging.socket;

import java.io.Writer;
import java.util.Map;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.JsonFormatter;

public class AdvancedJsonFormatter extends JsonFormatter {
    public Map<String, String> additionalFields;
    Writer writer;

    public AdvancedJsonFormatter(Map<Key, String> keyOverrides, Map<String, String> additionalFields) {
        super(keyOverrides);
        this.additionalFields = additionalFields;
    }

    @Override
    protected void before(Generator generator, ExtLogRecord record) throws Exception {
        if (!additionalFields.isEmpty()) {
            for (Map.Entry<String, String> entry : additionalFields.entrySet())
                generator.add(entry.getKey(), entry.getValue());
        }
        super.before(generator, record);
    }

}
