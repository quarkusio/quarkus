package io.quarkus.it.smallrye.config;

import io.quarkus.arc.Unremovable;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@Unremovable
@ConfigMapping(prefix = "cloud")
public interface Cloud {
    @WithParentName
    Server server();
}
