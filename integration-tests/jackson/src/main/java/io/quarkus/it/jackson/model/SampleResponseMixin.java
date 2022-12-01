package io.quarkus.it.jackson.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.jackson.JacksonMixin;

@JacksonMixin(SampleResponse.class)
public abstract class SampleResponseMixin {

    @JsonProperty("nm")
    public abstract String getName();
}
