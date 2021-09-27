package io.quarkus.it.smallrye.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "app")
public interface AppConfig {
    @WithDefault("app")
    String name();

    Info info();

    interface Info {
        @WithDefault("alias")
        String alias();

        @WithDefault("10")
        Integer count();
    }
}
