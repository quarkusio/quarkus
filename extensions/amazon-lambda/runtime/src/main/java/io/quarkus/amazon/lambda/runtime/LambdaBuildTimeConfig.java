package io.quarkus.amazon.lambda.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.lambda")
public interface LambdaBuildTimeConfig {

    /**
     * The exception classes expected to be thrown by the handler.
     *
     * Any exception thrown by the handler that is an instance of a class in this list will not be logged,
     * but will otherwise be handled normally by the lambda runtime. This is useful for avoiding unnecessary
     * stack traces while preserving the ability to log unexpected exceptions.
     */
    Optional<List<Class<?>>> expectedExceptions();
}
