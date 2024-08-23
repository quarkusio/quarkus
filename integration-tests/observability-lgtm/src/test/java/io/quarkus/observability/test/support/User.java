package io.quarkus.observability.test.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @JsonProperty
    public int id;
    @JsonProperty
    public String email;
    @JsonProperty
    public String login;
}
