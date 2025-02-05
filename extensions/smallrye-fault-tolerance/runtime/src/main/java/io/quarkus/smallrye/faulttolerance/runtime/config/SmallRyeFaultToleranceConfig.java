package io.quarkus.smallrye.faulttolerance.runtime.config;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Predicate;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.BeforeRetryHandler;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.CustomBackoffStrategy;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.api.RateLimitType;
import io.smallrye.faulttolerance.api.RetryWhen;

// this interface, as well as the nested interfaces, are never used;
// they only exist to signal to Quarkus that these config properties exist
@ConfigMapping(prefix = "quarkus.fault-tolerance")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SmallRyeFaultToleranceConfig {
    /**
     * Whether fault tolerance strategies are enabled. Note that {@code @Fallback}
     * is always enabled, this applies to all other strategies.
     */
    @ConfigDocDefault("true")
    Optional<Boolean> enabled();

    /**
     * Whether fault tolerance metrics are enabled.
     */
    @WithName("metrics.enabled")
    @ConfigDocDefault("true")
    Optional<Boolean> metricsEnabled();

    /**
     * Whether SmallRye Fault Tolerance should be compatible with the MicroProfile
     * Fault Tolerance specification.
     */
    @ConfigDocDefault("false")
    Optional<Boolean> mpCompatibility();

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
         * Configuration of the {@code @ApplyGuard} fault tolerance strategy.
         */
        Optional<ApplyGuardConfig> applyGuard();

        /**
         * Configuration of the {@code @Asynchronous} fault tolerance strategy.
         */
        Optional<AsynchronousConfig> asynchronous();

        /**
         * Configuration of the {@code @AsynchronousNonBlocking} fault tolerance strategy.
         */
        Optional<AsynchronousNonBlockingConfig> asynchronousNonBlocking();

        /**
         * Configuration of the {@code @BeforeRetry} fault tolerance strategy.
         */
        Optional<BeforeRetryConfig> beforeRetry();

        /**
         * Configuration of the {@code @Bulkhead} fault tolerance strategy.
         */
        Optional<BulkheadConfig> bulkhead();

        /**
         * Configuration of the {@code @CircuitBreaker} fault tolerance strategy.
         */
        Optional<CircuitBreakerConfig> circuitBreaker();

        /**
         * Configuration of the {@code @CustomBackoff} fault tolerance strategy.
         */
        Optional<CustomBackoffConfig> customBackoff();

        /**
         * Configuration of the {@code @ExponentialBackoff} fault tolerance strategy.
         */
        Optional<ExponentialBackoffConfig> exponentialBackoff();

        /**
         * Configuration of the {@code @Fallback} fault tolerance strategy.
         */
        Optional<FallbackConfig> fallback();

        /**
         * Configuration of the {@code @FibonacciBackoff} fault tolerance strategy.
         */
        Optional<FibonacciBackoffConfig> fibonacciBackoff();

        /**
         * Configuration of the {@code @RateLimit} fault tolerance strategy.
         */
        Optional<RateLimitConfig> rateLimit();

        /**
         * Configuration of the {@code @Retry} fault tolerance strategy.
         */
        Optional<RetryConfig> retry();

        /**
         * Configuration of the {@code @RetryWhen} fault tolerance strategy.
         */
        Optional<RetryWhenConfig> retryWhen();

        /**
         * Configuration of the {@code @Timeout} fault tolerance strategy.
         */
        Optional<TimeoutConfig> timeout();

        interface ApplyGuardConfig {
            /**
             * Whether the {@code @ApplyGuard} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();

            /**
             * The {@link io.smallrye.common.annotation.Identifier @Identifier}
             * of the {@link io.smallrye.faulttolerance.api.Guard Guard}
             * or {@link io.smallrye.faulttolerance.api.TypedGuard TypedGuard}
             * to use on the annotated method.
             *
             * @see ApplyGuard#value()
             */
            Optional<String> value();
        }

        interface AsynchronousConfig {
            /**
             * Whether the {@code @Asynchronous} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();
        }

        interface AsynchronousNonBlockingConfig {
            /**
             * Whether the {@code @AsynchronousNonBlocking} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();
        }

        interface BeforeRetryConfig {
            /**
             * Whether the {@code @BeforeRetry} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();

            /**
             * The class of the {@link BeforeRetryHandler} to call before retrying.
             *
             * @see BeforeRetry#value()
             */
            Optional<Class<? extends BeforeRetryHandler>> value();
        }

        interface BulkheadConfig {
            /**
             * Whether the {@code @Bulkhead} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();

            /**
             * The maximum number of concurrent invocations.
             *
             * @see Bulkhead#value()
             */
            @ConfigDocDefault("10")
            OptionalInt value();

            /**
             * The maximum number of queued asynchronous invocations. Asynchronous invocations are queued
             * when the number of concurrent invocations in progress has already reached the maximum.
             * Synchronous invocations are not queued at all and are rejected immediately.
             *
             * @see Bulkhead#waitingTaskQueue()
             */
            @ConfigDocDefault("10")
            OptionalInt waitingTaskQueue();
        }

        interface CircuitBreakerConfig {
            /**
             * Whether the {@code @CircuitBreaker} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();

            /**
             * The delay after which an open circuit breaker will move to half-open.
             *
             * @see CircuitBreaker#delay()
             */
            @ConfigDocDefault("5 seconds")
            OptionalLong delay();

            /**
             * The unit for {@link #delay()}.
             *
             * @see CircuitBreaker#delayUnit()
             */
            Optional<ChronoUnit> delayUnit();

            /**
             * The exception types that are considered failures.
             *
             * @see CircuitBreaker#failOn()
             */
            @ConfigDocDefault("Throwable (all exceptions)")
            Optional<Class<? extends Throwable>[]> failOn();

            /**
             * The ratio of failures within the rolling window that will move a closed circuit breaker to open.
             *
             * @see CircuitBreaker#failureRatio()
             */
            @ConfigDocDefault("0.5")
            OptionalDouble failureRatio();

            /**
             * The size of the circuit breaker rolling window.
             *
             * @see CircuitBreaker#requestVolumeThreshold()
             */
            @ConfigDocDefault("20")
            OptionalInt requestVolumeThreshold();

            /**
             * The exception types that are not considered failures. Takes priority over {@link #failOn()}.
             *
             * @see CircuitBreaker#skipOn()
             */
            @ConfigDocDefault("<empty set>")
            Optional<Class<? extends Throwable>[]> skipOn();

            /**
             * The number of successful executions that move a half-open circuit breaker to closed.
             *
             * @see CircuitBreaker#successThreshold()
             */
            @ConfigDocDefault("1")
            OptionalInt successThreshold();
        }

        interface CustomBackoffConfig {
            /**
             * Whether the {@code @CustomBackoff} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();

            /**
             * The class of the {@link CustomBackoffStrategy} that will be used to compute retry delays.
             *
             * @see CustomBackoff#value()
             */
            Optional<Class<? extends CustomBackoffStrategy>> value();
        }

        interface ExponentialBackoffConfig {
            /**
             * Whether the {@code @ExponentialBackoff} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();

            /**
             * The multiplicative factor used when determining a delay between two retries. A delay is computed
             * as {@code factor * previousDelay}, resulting in an exponential growth.
             *
             * @see ExponentialBackoff#factor()
             */
            @ConfigDocDefault("2")
            OptionalInt factor();

            /**
             * The maximum delay between retries.
             *
             * @see ExponentialBackoff#maxDelay()
             */
            @ConfigDocDefault("1 minute")
            OptionalLong maxDelay();

            /**
             * The unit for {@link #maxDelay()}.
             *
             * @see ExponentialBackoff#maxDelayUnit()
             */
            Optional<ChronoUnit> maxDelayUnit();
        }

        interface FallbackConfig {
            /**
             * Whether the {@code @Fallback} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();

            /**
             * The exception types that are considered failures and hence should trigger fallback.
             *
             * @see Fallback#applyOn()
             */
            @ConfigDocDefault("Throwable (all exceptions)")
            Optional<Class<? extends Throwable>[]> applyOn();

            /**
             * The exception types that are not considered failures and hence should not trigger fallback.
             * Takes priority over {@link #applyOn()}.
             *
             * @see Fallback#skipOn()
             */
            @ConfigDocDefault("<empty set>")
            Optional<Class<? extends Throwable>[]> skipOn();

            /**
             * The class of the {@link FallbackHandler} to call on fallback.
             *
             * @see Fallback#value()
             */
            Optional<Class<? extends FallbackHandler<?>>> value();
        }

        interface FibonacciBackoffConfig {
            /**
             * Whether the {@code @FibonacciBackoff} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();

            /**
             * The maximum delay between retries.
             *
             * @see FibonacciBackoff#maxDelay()
             */
            @ConfigDocDefault("1 minute")
            OptionalLong maxDelay();

            /**
             * The unit for {@link #maxDelay()}.
             *
             * @see FibonacciBackoff#maxDelayUnit()
             */
            Optional<ChronoUnit> maxDelayUnit();
        }

        interface RateLimitConfig {
            /**
             * Whether the {@code @RateLimit} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();

            /**
             * Minimum time between two consecutive invocations. If the time between two consecutive
             * invocations is shorter, the second invocation is rejected.
             *
             * @see RateLimit#minSpacing()
             */
            @ConfigDocDefault("0")
            OptionalLong minSpacing();

            /**
             * The unit for {@link #minSpacing()}.
             *
             * @see RateLimit#minSpacingUnit()
             */
            Optional<ChronoUnit> minSpacingUnit();

            /**
             * The type of type windows used for rate limiting.
             *
             * @see RateLimit#type()
             */
            @ConfigDocDefault("fixed")
            Optional<RateLimitType> type();

            /**
             * The maximum number of invocations in a time window.
             *
             * @see RateLimit#value()
             */
            @ConfigDocDefault("100")
            OptionalInt value();

            /**
             * The time window length.
             *
             * @see RateLimit#window()
             */
            @ConfigDocDefault("1 second")
            OptionalLong window();

            /**
             * The unit for {@link #window()}.
             *
             * @see RateLimit#windowUnit()
             */
            Optional<ChronoUnit> windowUnit();
        }

        interface RetryConfig {
            /**
             * Whether the {@code @Retry} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();

            /**
             * The exception types that are not considered failures and hence should not be retried.
             * Takes priority over {@link #retryOn()}.
             *
             * @see Retry#abortOn()
             */
            @ConfigDocDefault("<empty set>")
            Optional<Class<? extends Throwable>[]> abortOn();

            /**
             * The delay between retry attempts.
             *
             * @see Retry#delay()
             */
            @ConfigDocDefault("0")
            OptionalLong delay();

            /**
             * The unit for {@link #delay()}.
             *
             * @see Retry#delayUnit()
             */
            Optional<ChronoUnit> delayUnit();

            /**
             * The maximum jitter to apply for the delay between retry attempts.
             * The actual delay will be in the interval {@code [delay - jitter, delay + jitter]},
             * but will not be negative.
             *
             * @see Retry#jitter()
             */
            @ConfigDocDefault("200 millis")
            OptionalLong jitter();

            /**
             * The unit for {@link #jitter()}.
             *
             * @see Retry#jitterDelayUnit()
             */
            Optional<ChronoUnit> jitterUnit(); // `Retry.jitterDelayUnit()`

            /**
             * The maximum duration for which to retry.
             *
             * @see Retry#maxDuration()
             */
            @ConfigDocDefault("3 minutes")
            OptionalLong maxDuration();

            /**
             * The unit for {@link #maxDuration()}.
             *
             * @see Retry#durationUnit()
             */
            Optional<ChronoUnit> maxDurationUnit(); // `Retry.durationUnit()`

            /**
             * The maximum number of retry attempts.
             *
             * @see Retry#maxRetries()
             */
            @ConfigDocDefault("3")
            OptionalInt maxRetries();

            /**
             * The exception types that are considered failures and hence should be retried.
             *
             * @see Retry#retryOn()
             */
            @ConfigDocDefault("Exception (all exceptions)")
            Optional<Class<? extends Throwable>[]> retryOn();
        }

        interface RetryWhenConfig {
            /**
             * Whether the {@code @RetryWhen} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();

            /**
             * Class of the predicate that will be used to determine whether the invocation should be retried
             * if the guarded method has thrown an exception.
             *
             * @see RetryWhen#exception()
             */
            @ConfigDocDefault("AlwaysOnException")
            Optional<Class<? extends Predicate<Throwable>>> exception();

            /**
             * Class of the predicate that will be used to determine whether the invocation should be retried
             * if the guarded method has returned a result.
             *
             * @see RetryWhen#result()
             */
            @ConfigDocDefault("NeverOnResult")
            Optional<Class<? extends Predicate<Object>>> result();
        }

        interface TimeoutConfig {
            /**
             * Whether the {@code @Timeout} strategy is enabled.
             */
            @ConfigDocDefault("true")
            Optional<Boolean> enabled();

            /**
             * The unit for {@link #value()}.
             *
             * @see Timeout#unit()
             */
            Optional<ChronoUnit> unit();

            /**
             * The timeout to enforce.
             *
             * @see Timeout#value()
             */
            @ConfigDocDefault("1 second")
            OptionalLong value();
        }
    }
}
