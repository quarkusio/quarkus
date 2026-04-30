package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "status")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UnwrappedResult.Success.class, name = "success"),
        @JsonSubTypes.Type(value = UnwrappedResult.Failed.class, name = "failed")
})
public sealed interface UnwrappedResult
        permits UnwrappedResult.Success, UnwrappedResult.Failed {

    @JsonTypeName("success")
    record Success(@JsonUnwrapped Detail detail) implements UnwrappedResult {
    }

    @JsonTypeName("failed")
    record Failed(@JsonUnwrapped ErrorInfo error) implements UnwrappedResult {
    }
}
