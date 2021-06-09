package io.quarkus.it.smallrye.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "cloud")
public interface Cloud {
    @WithParentName
    Server server();
}
