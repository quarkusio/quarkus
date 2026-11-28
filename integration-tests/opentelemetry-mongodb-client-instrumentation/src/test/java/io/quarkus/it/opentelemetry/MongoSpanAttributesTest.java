package io.quarkus.it.opentelemetry;

import static io.opentelemetry.api.common.AttributeKey.stringArrayKey;
import static io.opentelemetry.semconv.DbAttributes.DB_QUERY_TEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;

class MongoSpanAttributesTest {
    @Test
    void queryTextCanBeAbsentButMustBeAStringWhenPresent() {
        assertThat(MongoSpanAttributes.optionalString(Attributes.empty(), DB_QUERY_TEXT)).isNull();
        assertThat(MongoSpanAttributes.optionalString(Attributes.of(DB_QUERY_TEXT, "find"), DB_QUERY_TEXT)).isEqualTo("find");
        Attributes mistyped = Attributes.of(stringArrayKey(DB_QUERY_TEXT.getKey()), List.of("find"));
        assertThatThrownBy(() -> MongoSpanAttributes.optionalString(mistyped, DB_QUERY_TEXT))
                .isInstanceOf(AssertionError.class).hasMessageContaining(DB_QUERY_TEXT.getKey());
    }
}
