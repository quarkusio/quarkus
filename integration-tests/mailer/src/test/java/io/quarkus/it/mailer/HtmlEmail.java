package io.quarkus.it.mailer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HtmlEmail {

    public String subject;
    public String html;
    public Recipient to;
}
