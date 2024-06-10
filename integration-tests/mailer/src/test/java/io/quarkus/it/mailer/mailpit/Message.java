package io.quarkus.it.mailer.mailpit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

    @JsonProperty("Attachments")
    public int attachmentCount;

    @JsonProperty("Bcc")
    public List<Recipient> bcc;

    @JsonProperty("Cc")
    public List<Recipient> cc;

    @JsonProperty("To")
    public List<Recipient> to;

    @JsonProperty("From")
    public Recipient from;

    @JsonProperty("ID")
    public String id;

    @JsonProperty("MessageID")
    public String messageId;

    @JsonProperty("Subject")
    public String subject;

    public Recipient to() {
        return to.get(0);
    }
}
