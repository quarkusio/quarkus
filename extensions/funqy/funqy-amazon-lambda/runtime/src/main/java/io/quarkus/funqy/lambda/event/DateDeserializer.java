package io.quarkus.funqy.lambda.event;

import java.io.IOException;
import java.util.Date;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * AWS uses double values. E.g. 1719318377.0
 * Therefore, a dedicated deserializer is needed
 */
public class DateDeserializer extends ValueDeserializer<Date> {

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        double timestamp = jsonParser.getDoubleValue();
        long milliseconds = (long) (timestamp * 1000);
        return new Date(milliseconds);
    }
}
