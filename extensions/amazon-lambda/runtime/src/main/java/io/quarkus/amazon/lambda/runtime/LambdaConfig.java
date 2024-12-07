package io.quarkus.amazon.lambda.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.lambda")
public interface LambdaConfig {

    /**
     * The handler name. Handler names are specified on handler classes using the {@link @jakarta.inject.Named} annotation.
     *
     * If this name is unspecified and there is exactly one unnamed implementation of
     * {@link com.amazonaws.services.lambda.runtime.RequestHandler}
     * then this unnamed handler will be used. If there is only a single named handler and the name is unspecified
     * then the named handler will be used.
     *
     */
    Optional<String> handler();
}
