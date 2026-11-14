package io.quarkus.oidc.client.registration.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.quarkus.oidc.client.registration.OidcClientRegistrationConfig;
import io.quarkus.oidc.client.registration.OidcClientRegistrations;
import io.smallrye.mutiny.Uni;

final class OidcClientRegistrationsImpl implements OidcClientRegistrations, Closeable {
    private OidcClientRegistration defaultClientReg;
    private Map<String, OidcClientRegistration> staticOidcClientRegs;
    Function<OidcClientRegistrationConfig, Uni<OidcClientRegistration>> newOidcClientReg;

    OidcClientRegistrationsImpl(OidcClientRegistration defaultClientReg,
            Map<String, OidcClientRegistration> staticOidcClientRegs,
            Function<OidcClientRegistrationConfig, Uni<OidcClientRegistration>> newOidcClientReg) {
        this.defaultClientReg = defaultClientReg;
        this.staticOidcClientRegs = staticOidcClientRegs;
        this.newOidcClientReg = newOidcClientReg;
    }

    @Override
    public OidcClientRegistration getClientRegistration() {
        return defaultClientReg;
    }

    @Override
    public OidcClientRegistration getClientRegistration(String id) {
        return staticOidcClientRegs.get(id);
    }

    public Map<String, OidcClientRegistration> getClientRegistrations() {
        return Collections.unmodifiableMap(staticOidcClientRegs);
    }

    @Override
    public Uni<OidcClientRegistration> newClientRegistration(OidcClientRegistrationConfig oidcConfig) {
        return newOidcClientReg.apply(oidcConfig);
    }

    @Override
    public void close() throws IOException {
        // a client registration is null when its configuration is not available
        if (defaultClientReg != null) {
            defaultClientReg.close();
        }
        for (OidcClientRegistration clientReg : staticOidcClientRegs.values()) {
            if (clientReg != null) {
                clientReg.close();
            }
        }
    }
}
