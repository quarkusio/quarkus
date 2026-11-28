package io.quarkus.it.keycloak;

import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.spiffe.svid.x509.X509Svid;
import io.quarkus.vertx.http.security.HttpSecurity;
import io.quarkus.vertx.http.security.MTLS;

@X509Svid(tls = SpiffeMtlsSetup.SERVER)
@X509Svid(tls = SpiffeMtlsSetup.CLIENT, spiffeId = "spiffe://test.quarkus.io/test-workload")
@X509Svid(tls = SpiffeMtlsSetup.OTHER_PEER_CLIENT, spiffeId = "spiffe://test.quarkus.io/other-workload")
class SpiffeMtlsSetup {

    static final String SERVER = "spiffe-server";
    static final String CLIENT = "spiffe-client";
    static final String OTHER_PEER_CLIENT = "spiffe-other-peer";

    void configure(@Observes HttpSecurity httpSecurity,
            @ConfigProperty(name = "spiffe.mtls.enabled", defaultValue = "false") boolean mtlsEnabled) {
        if (mtlsEnabled) {
            httpSecurity.mTLS(MTLS.required(SERVER)).path("/spiffe/mtls/server").mTLS();
        }
    }
}
