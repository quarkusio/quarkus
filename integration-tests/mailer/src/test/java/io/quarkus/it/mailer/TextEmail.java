package io.quarkus.it.mailer;

import java.util.List;

public class TextEmail {

    public String subject;
    public String text;
    public Recipient to;
    public List<Attachment> attachments;
    public List<Header> headerLines;
}
