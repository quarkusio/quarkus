package io.quarkus.smallrye.jwt.runtime.auth;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.security.identity.SecurityIdentity;

@RequestScoped
public class JsonWebTokenCredentialProducer {
    private static final Logger LOG = Logger.getLogger(JsonWebTokenCredentialProducer.class);
    @Inject
    SecurityIdentity identity;

    /**
     * The producer method for the current id token
     *
     * @return the id token
     */
    @Produces
    @RequestScoped
    JsonWebTokenCredential currentToken() {
        JsonWebTokenCredential cred = identity.getCredential(JsonWebTokenCredential.class);
        if (cred == null || cred.getToken() == null) {
            LOG.trace("JsonWebToken is null");
            cred = new JsonWebTokenCredential();
        }
        return cred;
    }
}
