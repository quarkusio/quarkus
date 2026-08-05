package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PolymorphicItem.TypeA.class, name = "type_a"),
})
public sealed interface PolymorphicItem permits PolymorphicItem.TypeA {

    @JsonTypeName("type_a")
    record TypeA(@JsonProperty("value") String value) implements PolymorphicItem {
    }
}