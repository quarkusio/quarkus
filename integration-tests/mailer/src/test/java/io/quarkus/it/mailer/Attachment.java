package io.quarkus.it.mailer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Attachment {

    public String contentDisposition;
    public String filename;
    public String contentType;
    public String checksum;

}
