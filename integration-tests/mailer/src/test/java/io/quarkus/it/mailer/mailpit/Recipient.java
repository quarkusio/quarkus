package io.quarkus.it.mailer.mailpit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Recipient(@JsonProperty("Address") String address, @JsonProperty("Name") String name) {

}
