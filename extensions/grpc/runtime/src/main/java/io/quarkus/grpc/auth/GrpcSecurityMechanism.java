package io.quarkus.grpc.auth;

import io.grpc.Metadata;
import io.quarkus.security.identity.request.AuthenticationRequest;

/**
 * gRPC security mechanism based on gRPC call metadata
 *
 * To secure your gRPC endpoints, create a CDI bean implementing this interface.
 *
 * Make sure that an {@link io.quarkus.security.identity.IdentityProvider} for the {@link AuthenticationRequest}
 * returned by {@code createAuthenticationRequest} is available by adding a suitable extension to your application.
 *
 */
public interface GrpcSecurityMechanism {
    int DEFAULT_PRIORITY = 1000;

    /**
     *
     * @param metadata metadata of the gRPC call
     * @return true if and only if the interceptor should handle security for this metadata. An interceptor may decide
     *         it should not be triggered for a call e.g. if some header is missing in metadata.
     */
    boolean handles(Metadata metadata);

    /**
     *
     * @param metadata metadata of the gRPC call
     * @return authentication request based on the metadata
     */
    AuthenticationRequest createAuthenticationRequest(Metadata metadata);

    default int getPriority() {
        return DEFAULT_PRIORITY;
    }
}
