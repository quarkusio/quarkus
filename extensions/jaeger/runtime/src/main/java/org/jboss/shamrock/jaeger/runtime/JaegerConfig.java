package org.jboss.shamrock.jaeger.runtime;

import java.util.Optional;

import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigGroup
@ConfigRoot(phase = ConfigPhase.STATIC_INIT)
public class JaegerConfig {

    /**
     * The service name
     */
    @ConfigItem
    public Optional<String> serviceName;

    /**
     * The jaeger service name
     */
    @ConfigItem(name="JAEGER_SERVICE_NAME")
    public Optional<String> jaegerServiceName;

    /**
     * The endpoint
     */
    @ConfigItem
    public Optional<String> endpoint;

}
