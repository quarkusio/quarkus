package io.quarkus.it.mailer.mailpit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Attachment {

    @JsonProperty("ContentID")
    public String contentId;

    @JsonProperty("ContentType")
    public String contentType;

    @JsonProperty("FileName")
    public String filename;

    @JsonProperty("PartID")
    public String partId;

    @JsonProperty("Size")
    public long size;

    public byte[] binary;

    public Attachment addBinary(byte[] binary) {
        this.binary = binary;
        return this;
    }
}
