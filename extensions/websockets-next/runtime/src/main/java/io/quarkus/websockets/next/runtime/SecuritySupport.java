package io.quarkus.websockets.next.runtime;

import java.util.Objects;

import jakarta.enterprise.inject.Instance;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;

public class SecuritySupport {

    static final SecuritySupport NOOP = new SecuritySupport(null, null);

    private final Instance<CurrentIdentityAssociation> currentIdentity;
    private final SecurityIdentity identity;

    SecuritySupport(Instance<CurrentIdentityAssociation> currentIdentity, SecurityIdentity identity) {
        this.currentIdentity = currentIdentity;
        this.identity = currentIdentity != null ? Objects.requireNonNull(identity) : identity;
    }

    /**
     * This method is called before an endpoint callback is invoked.
     */
    void start() {
        if (currentIdentity != null) {
            CurrentIdentityAssociation current = currentIdentity.get();
            current.setIdentity(identity);
        }
    }

}
