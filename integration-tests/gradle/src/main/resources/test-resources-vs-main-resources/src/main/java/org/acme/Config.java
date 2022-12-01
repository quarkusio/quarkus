package org.acme;

import io.quarkus.arc.config.ConfigProperties;

@ConfigProperties(prefix = "example")
public class Config {
    public String message;
}
