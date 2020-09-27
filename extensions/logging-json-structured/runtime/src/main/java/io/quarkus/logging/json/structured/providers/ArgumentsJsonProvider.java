package io.quarkus.logging.json.structured.providers;

import java.io.IOException;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.structured.JsonGenerator;
import io.quarkus.logging.json.structured.JsonProvider;
import io.quarkus.logging.json.structured.JsonStructuredConfig;

public class ArgumentsJsonProvider implements JsonProvider {
    private boolean includeStructuredArguments = true;
    private boolean includeNonStructuredArguments;
    private String nonStructuredArgumentsFieldPrefix = "arg";
    private String fieldName;

    public ArgumentsJsonProvider(JsonStructuredConfig config) {
        if (config.fields != null && config.fields.arguments != null) {
            JsonStructuredConfig.ArgumentsConfig arguments = config.fields.arguments;
            arguments.fieldName.ifPresent(f -> fieldName = f);
            this.includeStructuredArguments = arguments.includeStructuredArguments;
            this.includeNonStructuredArguments = arguments.includeNonStructuredArguments;
            this.nonStructuredArgumentsFieldPrefix = arguments.nonStructuredArgumentsFieldPrefix;
        }
    }

    @Override
    public void writeTo(JsonGenerator generator, ExtLogRecord event) throws IOException {

        if (!includeStructuredArguments && !includeNonStructuredArguments) {
            // Short-circuit if nothing is included
            return;
        }

        Object[] args = event.getParameters();

        if (args == null || args.length == 0) {
            return;
        }

        boolean hasWrittenFieldName = false;

        for (int argIndex = 0; argIndex < args.length; argIndex++) {

            Object arg = args[argIndex];

            if (arg instanceof Throwable) {
                continue;
            }

            if (arg instanceof StructuredArgument) {
                if (includeStructuredArguments) {
                    if (!hasWrittenFieldName && fieldName != null) {
                        generator.writeObjectFieldStart(fieldName);
                        hasWrittenFieldName = true;
                    }
                    StructuredArgument structuredArgument = (StructuredArgument) arg;
                    structuredArgument.writeTo(generator);
                }
            } else if (includeNonStructuredArguments) {
                if (!hasWrittenFieldName && fieldName != null) {
                    generator.writeObjectFieldStart(fieldName);
                    hasWrittenFieldName = true;
                }
                String innerFieldName = nonStructuredArgumentsFieldPrefix + argIndex;
                generator.writeObjectField(innerFieldName, arg);
            }
        }

        if (hasWrittenFieldName) {
            generator.writeEndObject();
        }
    }
}
