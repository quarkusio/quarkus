package io.quarkus.it.mailer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TextEmail {

    public String subject;
    public String text;
    public Recipient from;
    public Recipient to;
    public List<Attachment> attachments;
    public List<Header> headerLines;
}
