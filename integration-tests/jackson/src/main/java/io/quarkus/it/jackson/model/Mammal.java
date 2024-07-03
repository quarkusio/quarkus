package io.quarkus.it.jackson.model;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonTypeInfo(use = NAME, include = PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Elephant.class, name = "Elephant"),
        @JsonSubTypes.Type(value = Whale.class, name = "Whale"),
})
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public interface Mammal {
}
