package io.quarkus.smallrye.jwt.runtime;

import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.smallrye.jwt.runtime.auth.JWTAccount;

/**
 * Override the default CDI Principal bean to allow the injection of a Principal to be both a
 * {@linkplain JsonWebToken} and a {@linkplain java.security.Principal}.
 */
@Priority(1)
@Alternative
@RequestScoped
public class PrincipalProducer {
    private JWTAccount account;

    public PrincipalProducer() {
    }

    public JWTAccount getAccount() {
        return account;
    }

    public void setAccount(JWTAccount account) {
        this.account = account;
    }

    /**
     * The producer method for the current JsonWebToken
     *
     * @return
     */
    @Produces
    JsonWebToken currentJWTPrincipalOrNull() {
        JsonWebToken token = null;
        if (account != null) {
            token = (JsonWebToken) account.getPrincipal();
        }
        return token;
    }
}
