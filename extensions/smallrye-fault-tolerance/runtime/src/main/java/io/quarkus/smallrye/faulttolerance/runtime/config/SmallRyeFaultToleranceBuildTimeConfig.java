package io.quarkus.smallrye.faulttolerance.runtime.config;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.faulttolerance.Fallback;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;
import io.smallrye.faulttolerance.api.BeforeRetry;

// this interface, as well as the nested interfaces, are never used;
// they only exist to signal to Quarkus that these config properties exist
@ConfigMapping(prefix = "quarkus.fault-tolerance")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface SmallRyeFaultToleranceBuildTimeConfig {
    /**
     * Configuration of fault tolerance strategies; either global, per class, or per method.
     * Keys are:
     *
     * <ul>
     * <li>{@code global}: for global configuration</li>
     * <li>{@code "<classname>"}: for per class configuration</li>
     * <li>{@code "<classname>/<methodname>"}: for per method configuration</li>
     * </ul>
     *
     * Note that configuration follows the MicroProfile Fault Tolerance specification.
     * That is, if an annotation is present on a method, the configuration must be per method;
     * if an annotation is present on a class, the configuration must be per class.
     * Global configuration is a fallback for both per method and per class configuration,
     * but per class configuration is <em>not</em> a fallback for per method configuration.
     */
    @WithParentName
    @ConfigDocMapKey("<identifier>")
    Map<String, StrategiesConfig> strategies();

    interface StrategiesConfig {
        /**
         * Configuration of the {@code @BeforeRetry} fault tolerance strategy.
         */
        Optional<BeforeRetryConfig> beforeRetry();

        /**
         * Configuration of the {@code @Fallback} fault tolerance strategy.
         */
        Optional<FallbackConfig> fallback();

        interface BeforeRetryConfig {
            /**
             * The name of the method to call before retrying. The method belongs to the same class
             * as the guarded method. The method must have no parameters and return {@code void}.
             *
             * @see BeforeRetry#methodName()
             */
            Optional<String> methodName();
        }

        interface FallbackConfig {
            /**
             * The name of the method to call on fallback. The method belongs to the same class
             * as the guarded method. The method must have a signature matching the signature
             * of the guarded method.
             *
             * @see Fallback#fallbackMethod()
             */
            Optional<String> fallbackMethod();
        }
    }
}
