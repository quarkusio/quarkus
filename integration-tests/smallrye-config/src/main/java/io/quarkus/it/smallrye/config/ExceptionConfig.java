package io.quarkus.it.smallrye.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "exception")
public interface ExceptionConfig {
    String message();
}
