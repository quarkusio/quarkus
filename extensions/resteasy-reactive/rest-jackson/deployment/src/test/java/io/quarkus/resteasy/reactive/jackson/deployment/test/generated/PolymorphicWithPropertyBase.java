package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PolymorphicWithPropertyBase.TextItem.class),
        @JsonSubTypes.Type(value = PolymorphicWithPropertyBase.NumberItem.class)
})
public sealed interface PolymorphicWithPropertyBase {

    @JsonTypeName("text")
    record TextItem(
            @JsonProperty("text_value") String value,
            @JsonProperty("format") String format) implements PolymorphicWithPropertyBase {
    }

    @JsonTypeName("number")
    record NumberItem(
            @JsonProperty("num_value") int value) implements PolymorphicWithPropertyBase {
    }
}
