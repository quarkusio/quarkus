/* Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.util.Date;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.json.PackageVersion;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;

/**
 * Copied from: <a href=
 * "https://raw.githubusercontent.com/aws/aws-lambda-java-libs/95dc035b2a3200be04eaa48cef6053404250e547/aws-lambda-java-serialization/src/main/java/com/amazonaws/services/lambda/runtime/serialization/events/modules/DateModule.java">here</a>
 *
 * The AWS API represents a date as a double, which specifies the fractional
 * number of seconds since the epoch. Java's Date, however, represents a date as
 * a long, which specifies the number of milliseconds since the epoch. This
 * class is used to translate between these two formats.
 *
 * This class is copied from LambdaEventBridgeservice
 * com.amazon.aws.lambda.stream.ddb.DateModule
 */
class DateModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    public static final class Serializer extends ValueSerializer<Date> {
        @Override
        public void serialize(Date date, JsonGenerator generator, SerializationContext serializers) throws IOException {
            if (date != null) {
                generator.writeNumber(millisToSeconds(date.getTime()));
            }
        }
    }

    public static final class Deserializer extends ValueDeserializer<Date> {
        @Override
        public Date deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            double dateSeconds = parser.getValueAsDouble();
            if (dateSeconds == 0.0) {
                return null;
            } else {
                return new Date((long) secondsToMillis(dateSeconds));
            }
        }
    }

    private static double millisToSeconds(double millis) {
        return millis / 1000.0;
    }

    private static double secondsToMillis(double seconds) {
        return seconds * 1000.0;
    }

    public DateModule() {
        super(PackageVersion.VERSION);
        addSerializer(Date.class, new Serializer());
        addDeserializer(Date.class, new Deserializer());
    }
}
