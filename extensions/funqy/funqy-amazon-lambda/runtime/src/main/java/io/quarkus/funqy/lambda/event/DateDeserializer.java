package io.quarkus.funqy.lambda.event;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * AWS uses double values. E.g. 1719318377.0
 * Therefore, a dedicated deserializer is needed
 */
public class DateDeserializer extends JsonDeserializer<Date> {

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        double timestamp = jsonParser.getDoubleValue();
        long milliseconds = (long) (timestamp * 1000);
        return new Date(milliseconds);
    }
}
