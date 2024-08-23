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
     * Enables instrumentation for Messaging.
     */
    @WithDefault("true")
    boolean messaging();

    /**
     * Enables instrumentation for REST Client backed by RESTEasy Classic.
     */
    @WithDefault("true")
    boolean resteasyClient();

    /**
     * Enables instrumentation for Quarkus REST.
     */
    @WithDefault("true")
    boolean rest();

    /**
     * Enables instrumentation for RESTEasy Classic.
     */
    @WithDefault("true")
    boolean resteasy();

    // NOTE: agroal, graphql and scheduler have their own config properties

}
