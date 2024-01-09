package io.quarkus.opentelemetry.runtime.config.build;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface InstrumentBuildTimeConfig {

    /**
     * Enables instrumentation for gRPC.
     */
    @WithDefault("true")
    boolean grpc();

    /**
     * Enables instrumentation for SmallRye Reactive Messaging.
     */
    @WithDefault("true")
    boolean reactiveMessaging();

    /**
     * Enables instrumentation for JAX-RS Rest Client backed by RESTEasy Classic.
     */
    @WithDefault("true")
    boolean restClientClassic();

    /**
     * Enables instrumentation for RESTEasy Reactive.
     */
    @WithDefault("true")
    boolean resteasyReactive();

    /**
     * Enables instrumentation for RESTEasy Classic.
     */
    @WithDefault("true")
    boolean resteasyClassic();

    // NOTE: agroal, graphql and scheduler have their own config properties

}
