package io.quarkus.undertow.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class ServletConfig {

    /**
     * The context path to serve all Servlet context from. This will also affect any resources
     * that run as a Servlet, e.g. JAX-RS
     */
    @ConfigItem
    Optional<String> contextPath;

}
