package org.acme;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "example")
public interface Config {
    public String message();
}
