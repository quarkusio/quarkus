package io.quarkus.spiffe.client;

import io.quarkus.spiffe.svid.jwt.JwtSvidClient;
import io.quarkus.spiffe.svid.x509.X509SvidClient;
import io.smallrye.common.annotation.Experimental;

/**
 * Client for the SPIFFE Workload API.
 */
@Experimental("This API is currently experimental and might get changed")
public interface SpiffeClient extends X509SvidClient, JwtSvidClient {

}
