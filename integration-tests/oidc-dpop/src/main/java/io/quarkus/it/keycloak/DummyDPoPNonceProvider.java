package io.quarkus.it.keycloak;

import java.util.random.RandomGenerator;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.oidc.DPoPNonceProvider;

@ApplicationScoped
@LookupIfProperty(name = "use-dpop-nonce-provider", stringValue = "true")
public class DummyDPoPNonceProvider implements DPoPNonceProvider {

    private volatile String nonce = null;

    @Override
    public String getNonce() {
        if (nonce == null) {
            return String.valueOf(RandomGenerator.getDefault().nextInt());
        }
        return nonce;
    }

    @Override
    public boolean isValid(String nonce) {
        if (this.nonce == null) {
            return false;
        }
        return this.nonce.equals(nonce);
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
}
