package io.quarkus.amazon.lambda.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "lambda", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LambdaBuildTimeConfig {

    /**
     * The exception classes expected to be thrown by the handler.
     *
     * Any exception thrown by the handler that is an instance of a class in this list will not be logged,
     * but will otherwise be handled normally by the lambda runtime. This is useful for avoiding unnecessary
     * stack traces while preserving the ability to log unexpected exceptions.
     */
    @ConfigItem
    public Optional<List<Class<?>>> expectedExceptions;
}
