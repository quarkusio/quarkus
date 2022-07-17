package io.quarkus.it.mailer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Header {
    public String key;
    public String line;
}
