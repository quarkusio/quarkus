package io.quarkus.amazon.lambda.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.lambda")
public interface LambdaConfig {
    /**
     * The handler name. Handler names are specified on handler classes using the {@link @jakarta.inject.Named} annotation.
     * <p>
     * If this name is unspecified and there is exactly one unnamed implementation of
     * {@link com.amazonaws.services.lambda.runtime.RequestHandler}
     * then this unnamed handler will be used. If there is only a single named handler and the name is unspecified
     * then the named handler will be used.
     *
     */
    Optional<String> handler();

    /**
     * Configuration for the mock event server that is run
     * in dev mode and test mode
     */
    MockEventServerConfig mockEventServer();

    /**
     * Configuration for optional internal Lambda extension registration.
     */
    InternalExtensionConfig internalExtension();

    @ConfigGroup
    interface InternalExtensionConfig {
        /**
         * Enable registration of a lightweight internal extension against the Lambda Extensions API.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Extension name used in the {@code Lambda-Extension-Name} registration header.
         */
        @WithDefault("quarkus-internal-extension")
        String name();
    }
}
