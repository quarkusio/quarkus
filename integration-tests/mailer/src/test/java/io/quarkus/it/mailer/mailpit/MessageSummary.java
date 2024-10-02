package io.quarkus.it.mailer.mailpit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageSummary {

    @JsonProperty("messages")
    public List<Message> messages;

}
