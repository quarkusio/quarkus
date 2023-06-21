package io.quarkus.resteasy.reactive.common.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.resteasy-reactive")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface ResteasyReactiveConfig {

    /**
     * The amount of memory that can be used to buffer input before switching to
     * blocking IO.
     */
    @WithDefault("10k")
    MemorySize inputBufferSize();

    /**
     * The size of the chunks of memory allocated when writing data.
     * <p>
     * This is a very advanced setting that should only be set if you understand exactly how it affects the output IO operations
     * of the application.
     */
    @WithDefault("128")
    int minChunkSize();

    /**
     * The size of the output stream response buffer. If a response is larger than this and no content-length
     * is provided then the request will be chunked.
     * <p>
     * Larger values may give slight performance increases for large responses, at the expense of more memory usage.
     */
    @WithDefault("8191")
    int outputBufferSize();

    /**
     * By default, we assume a default produced media type of "text/plain"
     * for String endpoint return types. If this is disabled, the default
     * produced media type will be "[text/plain, *&sol;*]" which is more
     * expensive due to negotiation.
     */
    @WithDefault("true")
    boolean singleDefaultProduces();

    /**
     * When one of the quarkus-resteasy-reactive-jackson or quarkus-resteasy-reactive-jsonb extension are active
     * and the result type of an endpoint is an application class or one of {@code Collection}, {@code List}, {@code Set} or
     * {@code Map},
     * we assume the default return type is "application/json" if this configuration is enabled.
     */
    @WithDefault("true")
    @Experimental("This flag has a high probability of going away in the future")
    boolean defaultProduces();

    /**
     * Whether annotations such `@IfBuildTimeProfile`, `@IfBuildTimeProperty` and friends will be taken
     * into account when used on JAX-RS classes.
     */
    @WithDefault("true")
    boolean buildTimeConditionAware();

    /**
     * Whether duplicate endpoints should trigger error at startup
     */
    @WithDefault("true")
    boolean failOnDuplicate();
}
