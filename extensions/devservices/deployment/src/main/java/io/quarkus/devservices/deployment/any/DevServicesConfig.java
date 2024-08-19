package io.quarkus.devservices.deployment.any;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.devservices")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface DevServicesConfig {

    /**
     * DevServices Images.
     */
    @ConfigDocMapKey("service-name")
    @WithParentName
    @WithDefaults
    Map<String, DevServiceConfig> devservice();

}
