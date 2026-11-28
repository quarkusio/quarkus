package io.quarkus.it.opentelemetry;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

final class MongoSpanAttributes {
    private MongoSpanAttributes() {
    }

    static String optionalString(Attributes attributes, AttributeKey<String> key) {
        String value = attributes.get(key);
        if (value == null) {
            // A value recorded under the same name with a different type must fail the test.
            assertThat(attributes).doesNotContainKey(key.getKey());
        }
        return value;
    }
}
