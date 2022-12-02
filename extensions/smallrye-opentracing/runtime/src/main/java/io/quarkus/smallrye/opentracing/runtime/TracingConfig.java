package io.quarkus.smallrye.opentracing.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "opentracing", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class TracingConfig {
    /**
     *
     */
    @ConfigItem(name = "server.skip-pattern")
    public Optional<String> skipPattern;

    /**
     *
     */
    @ConfigItem(name = "server.operation-name-provider", defaultValue = "class-method")
    public Optional<OperationNameProvider> operationNameProvider;

    public enum OperationNameProvider {
        HTTP_PATH,
        CLASS_METHOD;
    }
}
