package io.quarkus.it.smallrye.config;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;

@StaticInitSafe
@ConfigMapping(prefix = "exception")
public interface ExceptionConfig {
    String message();
}
