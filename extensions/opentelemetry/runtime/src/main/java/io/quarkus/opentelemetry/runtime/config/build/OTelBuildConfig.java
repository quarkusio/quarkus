package io.quarkus.opentelemetry.runtime.config.build;

import static io.quarkus.opentelemetry.runtime.config.build.PropagatorType.Constants.BAGGAGE;
import static io.quarkus.opentelemetry.runtime.config.build.PropagatorType.Constants.TRACE_CONTEXT;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.SecurityEvent;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build Time configuration where all the attributes related with
 * classloading must live because of the native image needs
 */
@ConfigMapping(prefix = "quarkus.otel")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OTelBuildConfig {

    String INSTRUMENTATION_NAME = "io.quarkus.opentelemetry";

    /**
     * If false, disable the OpenTelemetry usage at build time. All other Otel properties will
     * be ignored at runtime.
     * <p>
     * Will pick up value from legacy property quarkus.opentelemetry.enabled
     * <p>
     * Defaults to <code>true</code>.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Trace exporter configurations.
     */
    TracesBuildConfig traces();

    /**
     * Metrics exporter configurations.
     */
    MetricsBuildConfig metrics();

    /**
     * Logs exporter configurations.
     */
    LogsBuildConfig logs();

    /**
     * The propagators to be used. Use a comma-separated list for multiple propagators.
     * <p>
     * Has values from {@link PropagatorType} or the full qualified name of a class implementing
     * {@link io.opentelemetry.context.propagation.TextMapPropagator}.
     * <p>
     * Default is {@value PropagatorType.Constants#TRACE_CONTEXT},{@value PropagatorType.Constants#BAGGAGE} (W3C).
     */
    @WithDefault(TRACE_CONTEXT + "," + BAGGAGE)
    List<String> propagators();

    /**
     * Enable/disable instrumentation for specific technologies.
     */
    InstrumentBuildTimeConfig instrument();

    /**
     * Allows to export Quarkus security events as the OpenTelemetry Span events.
     */
    SecurityEvents securityEvents();

    /**
     * Quarkus security events exported as the OpenTelemetry Span events.
     */
    @ConfigGroup
    interface SecurityEvents {
        /**
         * Whether exporting of the security events is enabled.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Selects security event types.
         */
        @WithDefault("ALL")
        List<SecurityEventType> eventTypes();

        /**
         * Security event type.
         */
        enum SecurityEventType {
            /**
             * All the security events.
             */
            ALL(SecurityEvent.class),
            /**
             * Authentication success event.
             */
            AUTHENTICATION_SUCCESS(AuthenticationSuccessEvent.class),
            /**
             * Authentication failure event.
             */
            AUTHENTICATION_FAILURE(AuthenticationFailureEvent.class),
            /**
             * Authorization success event.
             */
            AUTHORIZATION_SUCCESS(AuthorizationSuccessEvent.class),
            /**
             * Authorization failure event.
             */
            AUTHORIZATION_FAILURE(AuthorizationFailureEvent.class),
            /**
             * Any other security event. For example the OpenId Connect security event belongs here.
             */
            OTHER(SecurityEvent.class);

            private final Class<? extends SecurityEvent> observedType;

            SecurityEventType(Class<? extends SecurityEvent> observedType) {
                this.observedType = observedType;
            }

            public Class<? extends SecurityEvent> getObservedType() {
                return observedType;
            }
        }
    }
}
