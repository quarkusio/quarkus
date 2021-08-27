package io.quarkus.resteasy.reactive.common.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.common.annotation.Experimental;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED, name = "resteasy-reactive")
public class ResteasyReactiveConfig {

    /**
     * The amount of memory that can be used to buffer input before switching to
     * blocking IO.
     */
    @ConfigItem(defaultValue = "10k")
    public MemorySize inputBufferSize;

    /**
     * By default we assume a default produced media type of "text/plain"
     * for String endpoint return types. If this is disabled, the default
     * produced media type will be "[text/plain, *&sol;*]" which is more
     * expensive due to negotiation.
     */
    @ConfigItem(defaultValue = "true")
    public boolean singleDefaultProduces;

    /**
     * When one of the quarkus-resteasy-reactive-jackson or quarkus-resteasy-reactive-jsonb extension are active
     * and the result type of an endpoint is an application class or one of {@code Collection}, {@code List}, {@code Set} or
     * {@code Map},
     * we assume the default return type is "application/json" if this configuration is enabled.
     */
    @ConfigItem(defaultValue = "true")
    @Experimental("This flag has a high probability of going away in the future")
    public boolean defaultProduces;

    /**
     * Whether or not annotations such `@IfBuildTimeProfile`, `@IfBuildTimeProperty` and friends will be taken
     * into account when used on JAX-RS classes.
     */
    @ConfigItem(defaultValue = "true")
    public boolean buildTimeConditionAware;

    /**
     * If set to true, access to all JAX-RS resources will be denied by default
     *
     * Use quarkus.security.jaxrs.deny-unannotated-endpoints instead
     */
    @Deprecated(forRemoval = true)
    @ConfigItem(name = "deny-unannotated-endpoints")
    public Optional<Boolean> denyJaxRs;

}
