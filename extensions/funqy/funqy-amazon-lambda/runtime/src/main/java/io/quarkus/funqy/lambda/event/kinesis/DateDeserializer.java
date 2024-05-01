package io.quarkus.funqy.lambda.event.kinesis;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class DateDeserializer extends JsonDeserializer<Date> {

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String fieldName = jsonParser.getCurrentName();
        if ("approximateArrivalTimestamp".equals(fieldName)) {
            double timestamp = jsonParser.getDoubleValue();
            long milliseconds = (long) (timestamp * 1000);
            return new Date(milliseconds);
        }
        // For other properties, delegate to default deserialization
        return (Date) deserializationContext.handleUnexpectedToken(Date.class, jsonParser);
    }
}
