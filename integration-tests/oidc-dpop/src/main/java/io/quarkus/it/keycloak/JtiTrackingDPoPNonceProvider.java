package io.quarkus.it.keycloak;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.oidc.DPoPNonceProvider;

@ApplicationScoped
@LookupIfProperty(name = "use-jti-dpop-nonce-provider", stringValue = "true")
public class JtiTrackingDPoPNonceProvider implements DPoPNonceProvider {

    private volatile String nonce = null;
    private volatile String lastGetNonceJti = null;
    private final Set<String> usedJtis = ConcurrentHashMap.newKeySet();

    @Override
    public String getNonce(DPoPNonceContext context) {
        // The jti is unverified here; kept only so the test can assert it was made available.
        this.lastGetNonceJti = context.jti();
        return nonce;
    }

    @Override
    public boolean isValid(DPoPNonceContext context) {
        if (nonce == null || !nonce.equals(context.nonce())) {
            return false;
        }
        return usedJtis.add(context.jti());
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getLastGetNonceJti() {
        return lastGetNonceJti;
    }

    public void clear() {
        this.nonce = null;
        this.lastGetNonceJti = null;
        usedJtis.clear();
    }
}
